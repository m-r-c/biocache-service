<%-- 
    Document   : show
    Created on : Apr 21, 2010, 9:36:39 AM
    Author     : "Nick dos Remedios <Nick.dosRemedios@csiro.au>"
--%>

<%@ page contentType="text/html" pageEncoding="UTF-8" %>
<%@ include file="/common/taglibs.jsp" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Occurrence Record: ${id}</title>
    </head>
    <body onload="initialize()">
        <h1>Occurrence Details: ${occurrence.institutionCode} ${occurrence.collectionCode} ${occurrence.catalogueNumber}</h1>
        <c:if test="${not empty occurrence.latitude && not empty occurrence.longitude}">
            <script type="text/javascript" src="http://maps.google.com/maps/api/js?sensor=false"></script>
            <script type="text/javascript">
                $(document).ready(function() {
                    var latlng = new google.maps.LatLng(${occurrence.latitude}, ${occurrence.longitude});
                    var myOptions = {
                        zoom: 4,
                        center: latlng,
                        scaleControl: true,
                        mapTypeId: google.maps.MapTypeId.ROADMAP
                    };

                    var map = new google.maps.Map(document.getElementById("occurrenceMap"), myOptions);

                    var marker = new google.maps.Marker({
                        position: latlng,
                        map: map,
                        title:"Occurence Location"
                    });
                });
            </script>
            <div id="occurrenceMap"></div>
        </c:if>
        <div id="occurrenceDataset" class="occurrenceSection">
            <h2>Datset</h2>
            <table class="occurrenceTable">
                <alatag:occurrenceTableRow fieldName="Data Provider">${occurrence.dataProvider}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Data Set">${occurrence.dataResource}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Institution Code">${occurrence.institutionCode}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Collection Code">${occurrence.collectionCode}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Catalogue Number">${occurrence.catalogueNumber}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Basis of Record">${occurrence.basisOfRecord}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Record Date"><fmt:formatDate value="${occurrence.occurrenceDate}" pattern="yyyy-MM-dd"/></alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="${occurrence.identifierType}" fieldNameIsMsgCode="true">${occurrence.identifierValue}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Identifier">${occurrence.identifierName}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Identified Date"><fmt:formatDate value="${occurrence.identifierDate}" pattern="yyyy-MM-dd"/></alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Collector">${occurrence.collector}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Type Status">${occurrence.typeStatus}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="taxonomic Issue"><c:if test="${occurrence.taxonomicIssue != 0}">${occurrence.taxonomicIssue}</c:if></alatag:occurrenceTableRow>
            </table>
        </div>
        <div id="occurrenceTaxonomy" class="occurrenceSection">
            <h2>Taxonomy</h2>
            <table class="occurrenceTable">
                <alatag:occurrenceTableRow fieldName="Scientific Name">
                    <alatag:formatSciName rankId="${occurrence.rankId}" name="${occurrence.rawTaxonName}"/> ${occurrence.rawAuthor}
                    <c:if test="${!fn:containsIgnoreCase(occurrence.taxonName, occurrence.rawTaxonName)}">
                        (interpreted as <alatag:formatSciName rankId="${occurrence.rankId}" name="${occurrence.taxonName}"/> ${occurrence.author})
                    </c:if>
                </alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Taxon Rank">
                    <span style="text-transform: capitalize;">${occurrence.rank}</span>
                </alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Kingdom">${occurrence.kingdom}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Family">${occurrence.family}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Genus">${occurrence.genus}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Species">${occurrence.species}</alatag:occurrenceTableRow>
            </table>
        </div>
        <div id="occurrenceGeospatial" class="occurrenceSection">
            <h2>Geospatial</h2>
            <table class="occurrenceTable">
                <alatag:occurrenceTableRow fieldName="Country">${occurrence.countryCode}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="State/Province">${occurrence.state}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Biogeographic Region">${occurrence.biogeographicRegion}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Places">${occurrence.place}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Latitude">${occurrence.latitude}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Longitude">${occurrence.longitude}</alatag:occurrenceTableRow>
                <alatag:occurrenceTableRow fieldName="Coordinate Precision">${occurrence.coordinatePrecision}</alatag:occurrenceTableRow>
            </table>
        </div>

    </body>
</html>
