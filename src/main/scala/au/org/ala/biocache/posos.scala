package au.org.ala.biocache
import org.apache.commons.lang.StringUtils
import java.lang.reflect.Method
import scala.reflect.BeanProperty
import scala.collection.mutable.HashMap

/**
 * Holds the details of a property for a bean
 */
case class ModelProperty(name: String, getter: Method, setter: Method)

/**
 * A singleton that keeps a cache of POSO metadata.
 */
object ReflectionCache {
    
    var posoLookupCache = new HashMap[Class[_], Map[String, ModelProperty]]
    var compositeLookupCache = new HashMap[Class[_], Map[String, Method]]
    
    def getCompositeLookup(cposo:CompositePOSO) : Map[String, Method] ={
        
        val result = compositeLookupCache.get(cposo.getClass)
        
        if(result.isEmpty){
	        val map = new HashMap[String,Method]()
	        cposo.getClass.getDeclaredFields.map(field => {
	            val name = field.getName;
	            val typ = field.getType;
	            try {
	                val getter = cposo.getClass.getDeclaredMethod("get" + StringUtils.capitalize(name));
	                val isAPoso = !(getter.getReturnType.getInterfaces.forall(i => i == classOf[POSO]))
	                if(isAPoso){
	                	val poso = getter.invoke(cposo).asInstanceOf[POSO]
	                	poso.propertyNames.foreach(name => map += (name -> getter) )
	                }
	            } catch {
	                case e: Exception => 
	            }
	           
	        })
	        val fieldMap = map.toMap
	        compositeLookupCache.put(cposo.getClass, fieldMap)
	        fieldMap
        } else {
            result.get
        }
    }
    
    def getPosoLookup(poso:POSO): Map[String, ModelProperty] = {
        
        val result = posoLookupCache.get(poso.getClass)
        
        if(result.isEmpty){
	        val posoLookupMap = poso.getClass.getDeclaredFields.map(field => {
		        val name = field.getName
		        try {
		            val getter = poso.getClass.getDeclaredMethod("get" + StringUtils.capitalize(name))
		            val setter = poso.getClass.getDeclaredMethod("set" + StringUtils.capitalize(name), field.getType)
		            Some((name -> ModelProperty(name, getter, setter)))
		        } catch {
		            case e: Exception => None
		        }
	        }).filter(x => !x.isEmpty).map(y => y.get).toMap
	        
	        posoLookupCache.put(poso.getClass, posoLookupMap)
	        posoLookupMap
        } else {
            result.get
        }
    }
}

trait CompositePOSO extends POSO {

    
    val posoGetterLookup = ReflectionCache.getCompositeLookup(this)
    val nestedProperties = posoGetterLookup.keys
    

    def getNestedProperty(name:String) : Option[String] = {
    	val getter = posoGetterLookup.get(name)
    	getter match {
    	    case Some(method) => {
    	        val poso = method.invoke(this).asInstanceOf[POSO]
    	        poso.getProperty(name)
    	    }
    	    case None => println("Unrecognised property"); None
    	}
    }
    
    def setNestedProperty(name:String, value:String){
    	val getter = posoGetterLookup.get(name)
    	getter match {
    	    case Some(method) => {
    	        val poso = method.invoke(this).asInstanceOf[POSO]
    	        poso.setProperty(name, value)
    	    }
    	    case None => println("Unrecognised property"); None
    	}
    }
}

trait POSO {

    private val lookup = ReflectionCache.getPosoLookup(this)
    val propertyNames = lookup.keys

    def setProperty(name: String, value: String) = lookup.get(name) match {
        case Some(property) => property.setter.invoke(this, value)
        case None => println("Property not mapped")
    }

    def getProperty(name: String): Option[String] = lookup.get(name) match {
        case Some(property) => Some(property.getter.invoke(this).toString)
        case None => println("Property not mapped"); None;
    }
}


class BeanTest extends POSO {
    @BeanProperty
    var property1: String = _
    @BeanProperty
    var property2: String = _
}

object Test222 {

    def main(args: Array[String]) {
        val fr = new FullRecord
        println("##########Properties")
        fr.propertyNames.foreach(p => println(p))
        
        println("\n\n##########Nested Properties")
        fr.nestedProperties.foreach(p => println(p))
        
        fr.setNestedProperty("scientificName", "Aus bus")
        println("Retrieved name: "+ fr.getNestedProperty("scientificName"))
        println("Retrieved name: "+ fr.classification.scientificName)
    }
}