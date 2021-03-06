/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 * 
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 * 
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.biocache.util;

import au.org.ala.biocache.Store;
import au.org.ala.biocache.dto.IndexFieldDTO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * Stores the download fields whose values can be overridden in
 * a properties file.
 *
 * @author "Natasha Carter <Natasha.Carter@csiro.au>"
 */
public class DownloadFields {
	
    private final static Logger logger = LoggerFactory.getLogger(DownloadFields.class);	

    private String defaultFields = "uuid,dataResourceUid,catalogNumber,taxonConceptID.p,scientificName,vernacularName,scientificName.p," +
            "taxonRank.p,vernacularName.p,kingdom.p,phylum.p,classs.p,order.p,family.p,genus.p,species.p,subspecies.p," +
            "institutionCode,collectionCode,longitude.p,latitude.p,coordinatePrecision,country.p,ibra.p,imcra.p," +
            "stateProvince.p,lga.p,minimumElevationInMeters,maximumElevationInMeters,minimumDepthInMeters," +
            "maximumDepthInMeters,year.p,month.p,day.p,eventDate.p,eventTime.p,basisOfRecord,typeStatus.p,sex,preparations";

    private Properties downloadProperties;
    private Map<String,IndexFieldDTO> indexFieldMaps;

    public DownloadFields(Set<IndexFieldDTO> indexFields){
        //initialise the properties
        try {
            downloadProperties = new Properties();
            InputStream is = getClass().getResourceAsStream("/download.properties");
            downloadProperties.load(is);
            indexFieldMaps = new TreeMap<String,IndexFieldDTO>();
            for(IndexFieldDTO field: indexFields){
                indexFieldMaps.put(field.getName(), field);
            }
        } catch(Exception e) {
        	logger.error(e.getMessage(), e);
        }
        
        if(downloadProperties.getProperty("fields") == null){
            downloadProperties.setProperty("fields", defaultFields);
        }
    }

    /**
     * Get the name of the field that should be included in the download.
     * @return
     */
    public String getFields(){
        return downloadProperties.getProperty("fields");
    }

    /**
     * Gets the header for the file
     * @param values
     * @return
     */
    public String[] getHeader(String[] values, boolean useSuffix){
        String[] header = new String[values.length];
        for(int i =0; i < values.length; i++){
            //attempt to get the headervalue from the properties
            String v = downloadProperties.getProperty(values[i]);
            header[i] = v != null ? v : generateTitle(values[i], useSuffix);
        }
        return header;
    }

    /**
     * Generates a default title for a field that does NOT have an i18n
     * @param value
     * @return
     */
    private String generateTitle(String value, boolean useSuffix){
        String suffix = "";
        if(value.endsWith(".p")){
            suffix = " - Processed";
            value = value.replaceAll("\\.p", "");
        }
        value = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(value), " ");
        if(useSuffix) {
            value += suffix;
        }
        return value;
    }

    /**
     * Returns the index fields that are used for the supplied values.
     *
     * @param values
     * @return
     */
    public List<String>[] getIndexFields(String[] values){
        java.util.List<String> mappedNames = new java.util.LinkedList<String>();
        java.util.List<String> headers = new java.util.LinkedList<String>();
        java.util.List<String> unmappedNames = new java.util.LinkedList<String>();
        java.util.Map<String, String> storageFieldMap = Store.getStorageFieldMap();
        for(String value : values){
            //check to see if it is the the
            String indexName = storageFieldMap.containsKey(value) ? storageFieldMap.get(value) : value;
            //now check to see if this index field is stored
            IndexFieldDTO field = indexFieldMaps.get(indexName);
            if((field != null && field.isStored()) || value.startsWith("sensitive")){
                mappedNames.add(indexName);
                headers.add(downloadProperties.getProperty(value, generateTitle(value,true)));
            } else {
                unmappedNames.add(indexName);
            }
        }
        return new List[]{mappedNames,unmappedNames,headers};
    }
}
