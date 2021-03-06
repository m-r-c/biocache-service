package au.org.ala.biocache.dao;
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

import au.org.ala.biocache.dto.*;
import au.org.ala.biocache.util.LegendItem;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.common.SolrDocumentList;

import javax.servlet.ServletOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DAO for searching occurrence records held in the biocache.
 *
 * @author "Nick dos Remedios <Nick.dosRemedios@csiro.au>" .
 */
public interface SearchDAO {

    /**
     * Finds the endemic species of the supplied area.
     * 
     * @param requestParams
     * @return
     * @throws Exception
     */
    List<FieldResultDTO> getEndemicSpecies(SpatialSearchRequestParams requestParams) throws Exception;

    /**
     * Retrieve a list of facet values for the supplied query.
     *
     * @param requestParams
     * @return
     * @throws Exception
     */
    List<FieldResultDTO> getValuesForFacet(SpatialSearchRequestParams requestParams) throws Exception;

    /**
     * Find all occurrences for a given (full text) query, latitude, longitude & radius (km). I.e.
     * a full-text spatial query.
     *
     * @param requestParams
     * @return
     * @throws Exception
     */
    SearchResultDTO findByFulltextSpatialQuery(SpatialSearchRequestParams requestParams, Map<String,String[]> extraParams);
    /**
     * Find all occurrences for a given (full text) query, latitude, longitude & radius (km). I.e.
     * a full-text spatial query.  The result will include the sensitive coordinates if available.
     *
     * @param requestParams
     * @return
     * @throws Exception
     */
    SearchResultDTO findByFulltextSpatialQuery(SpatialSearchRequestParams requestParams, boolean includeSensitive, Map<String, String[]> extraParams);

    /**
     * Writes the species count in the specified circle to the output stream.
     * @param requestParams
     * @param speciesGroup
     * @param out
     * @return
     * @throws Exception
     */
    int writeSpeciesCountByCircleToStream(SpatialSearchRequestParams requestParams, String speciesGroup, ServletOutputStream out) throws Exception;

    /**
     * Write out the results of this query to the output stream
     * 
     * @param searchParams
     * @param out
     * @param maxNoOfRecords
     * @param includeSensitive Whether or not the sensitive values should be included in the download
     * @return A map of uids and counts that needs to be logged to the ala-logger
     * @throws Exception
     */
	Map<String,Integer> writeResultsToStream(DownloadRequestParams searchParams, OutputStream out, int maxNoOfRecords, boolean includeSensitive, DownloadDetailsDTO dd) throws Exception;
	
	/**
	 * Writes the results of this query to the output stream using the index as a source of the data.
	 * @param downloadParams
	 * @param out
	 * @param includeSensitive
	 * @return
	 * @throws Exception
	 */
	Map<String, Integer> writeResultsFromIndexToStream(DownloadRequestParams downloadParams, OutputStream out, boolean includeSensitive, DownloadDetailsDTO dd,boolean checkLimit) throws Exception;

    /**
     * Write coordinates out to the supplied stream.
     *
     * @param searchParams
     * @param out
     * @throws Exception
     */
    void writeCoordinatesToStream(SearchRequestParams searchParams,OutputStream out) throws Exception;

    /**
     * Write facet content to supplied output stream
     *
     * @param searchParams
     * @param includeCount
     * @param lookupName
     * @param includeSynonyms
     * @param out
     * @param dd
     * @throws Exception
     */
    void writeFacetToStream(SpatialSearchRequestParams searchParams, boolean includeCount, boolean lookupName, boolean includeSynonyms, OutputStream out, DownloadDetailsDTO dd) throws Exception;

    /**
     * Retrieve a list of the indexed fields.
     *
     * @return
     * @throws Exception
     */
    Set<IndexFieldDTO> getIndexedFields() throws Exception;
    
    /**
     * Returns the up to date statistics for the supplied field
     * @param field
     * @return
     * @throws Exception
     */
    Set<IndexFieldDTO> getIndexFieldDetails(String... field) throws Exception;

    /**
     * Retrieve an OccurrencePoint (distinct list of points - lat-long to 4 decimal places) for a given search
     *
     * @param searchParams
     * @param pointType
     * @return
     * @throws Exception
     */
    List<OccurrencePoint> getFacetPoints(SpatialSearchRequestParams searchParams, PointType pointType) throws Exception;

    /**
     * Retrieve a list of occurrence uid's for a given search
     *
     * @param requestParams
     * @param pointType
     * @param colourBy
     * @param searchType
     * @return
     * @throws Exception
     */
    List<OccurrencePoint> getOccurrences(SpatialSearchRequestParams requestParams, PointType pointType, String colourBy, int searchType) throws Exception;

    /**
     * Get a list of occurrence points for a given lat/long and distance (radius)
     *
     * @param requestParams
     * @param pointType
     * @return
     * @throws Exception
     */
    List<OccurrencePoint> findRecordsForLocation(SpatialSearchRequestParams requestParams, PointType pointType) throws Exception;

    /**
     * Refresh any caches in use to populate queries.
     */
    void refreshCaches();

    /**
     * Find all species (and counts) for a given location search (lat/long and radius) and a higher taxon (with rank)
     *
     * @param requestParams
     * @param speciesGroup
     * @return
     * @throws Exception
     */
    List<TaxaCountDTO> findAllSpeciesByCircleAreaAndHigherTaxa(SpatialSearchRequestParams requestParams, String speciesGroup) throws Exception;

    /**
     * Find all the data providers with records.
     * 
     * @return
     */
    List<DataProviderCountDTO> getDataProviderCounts() throws Exception;

    List<FieldResultDTO> findRecordByStateFor(String query) throws Exception;

    /**
     * Find all the sources for the supplied query
     *
     * @param searchParams
     * @return
     * @throws Exception
     */
    Map<String,Integer> getSourcesForQuery(SpatialSearchRequestParams searchParams) throws Exception;

    /**
     * Calculate taxon breakdown.
     *
     * @param queryParams
     * @return
     * @throws Exception
     */
    TaxaRankCountDTO calculateBreakdown(BreakdownRequestParams queryParams) throws Exception;

    /**
     * Returns the occurrence counts based on lft and rgt values for each of the supplied taxa.
     * @param taxa
     * @return
     * @throws Exception
     */
    Map<String, Integer> getOccurrenceCountsForTaxa(List<String> taxa) throws Exception;

    /**
     * Returns the scientific name and counts for the taxon rank that proceed or include the supplied rank.
     * @param breakdownParams
     * @param query
     * @return
     * @throws Exception
     */
    TaxaRankCountDTO findTaxonCountForUid(BreakdownRequestParams breakdownParams,String query) throws Exception;

    /**
     * Find all species (and counts) for a given query.
     * @param requestParams
     * @return
     * @throws Exception
     */
    List<TaxaCountDTO> findAllSpecies(SpatialSearchRequestParams requestParams) throws Exception;
    
    /**
     * Find all occurrences for a given query as SolrDocumentList
     *
     * @param searchParams
     * @return
     * @throws Exception
     */
    SolrDocumentList findByFulltext(SpatialSearchRequestParams searchParams) throws Exception;
    /**
     * Statistics for each of the fields included as facets.  Statistics are only possible for numeric fields.
     * @param searchParams
     * @return
     * @throws Exception
     */
    Map<String, FieldStatsInfo> getStatistics(SpatialSearchRequestParams searchParams) throws Exception;

    /**
     * Get legend items for a query and specified facet.
     *
     * Continuous variable cut-points can be specified.  Includes the minimum
     * and maximum values.
     *
     * Returns an empty list if no valid values are found.
     *
     * @param searchParams
     * @param facet
     * @return
     * @throws Exception
     */
    List<LegendItem> getLegend(SpatialSearchRequestParams searchParams, String facet, String [] cutpoints) throws Exception;

    /**
     * Get a data provider list for a query.
     *
     * @param requestParams
     * @return
     * @throws Exception
     */
    List<DataProviderCountDTO> getDataProviderList(SpatialSearchRequestParams requestParams) throws Exception;

    /**
     * Retrieve facet counts for this query
     *
     * @param searchParams
     * @return
     * @throws Exception
     */
    List<FacetResultDTO> getFacetCounts(SpatialSearchRequestParams searchParams) throws Exception;
}