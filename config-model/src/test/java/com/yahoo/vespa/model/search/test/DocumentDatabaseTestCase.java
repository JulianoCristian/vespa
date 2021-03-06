// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.model.search.test;

import com.google.common.collect.ImmutableMap;
import com.yahoo.vespa.config.search.IndexschemaConfig;
import com.yahoo.vespa.config.search.core.ProtonConfig;
import com.yahoo.vespa.config.search.RankProfilesConfig;
import com.yahoo.prelude.fastsearch.DocumentdbInfoConfig;
import com.yahoo.search.config.IndexInfoConfig;
import com.yahoo.searchdefinition.parser.ParseException;
import com.yahoo.vespa.config.search.AttributesConfig;
import com.yahoo.vespa.configdefinition.IlscriptsConfig;
import com.yahoo.vespa.model.VespaModel;
import com.yahoo.vespa.model.content.ContentSearchCluster;
import com.yahoo.vespa.model.search.IndexedSearchCluster;
import com.yahoo.vespa.model.test.utils.ApplicationPackageUtils;
import com.yahoo.vespa.model.test.utils.VespaModelCreatorWithMockPkg;
import org.junit.Test;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author geirst
 */
public class DocumentDatabaseTestCase {

    private String vespaHosts = "<?xml version='1.0' encoding='utf-8' ?>" +
            "<hosts>  " +
            "  <host name='foo'>" +
            "    <alias>node0</alias>" +
            "  </host>" +
            "</hosts>";

    private String createVespaServices(List<String> sdNames, String mode) {
        StringBuilder retval = new StringBuilder();
        retval.append("" +
                      "<?xml version='1.0' encoding='utf-8' ?>\n" +
                      "<services version='1.0'>\n" +
                      "<admin version='2.0'>\n" +
                      "   <adminserver hostalias='node0' />\n" +
                      "</admin>\n" +
                      "<container version='1.0'>\n" +
                      "   <nodes>\n" +
                      "      <node hostalias='node0'/>\n" +
                      "   </nodes>\n" +
                      "   <search/>\n" +
                      "</container>\n" +
                      "<content version='1.0' id='test'>\n" +
                      "   <redundancy>1</redundancy>\n");
        retval.append("   <documents>\n");
        for (String sdName : sdNames) {
            retval.append("").append("      <document type='").append(sdName).append("' mode='").append(mode).append("'");
            retval.append("/>\n");
        }
        retval.append("   </documents>\n");
        retval.append("" +
                      "   <nodes>\n" +
                      "      <node hostalias='node0' distribution-key='0'/>\n" +
                      "   </nodes>\n" +
                      "</content>\n" +
                      "</services>\n");
        return retval.toString();
    }

    private ProtonConfig getProtonCfg(ContentSearchCluster cluster) {
        ProtonConfig.Builder pb = new ProtonConfig.Builder();
        cluster.getConfig(pb);
        return new ProtonConfig(pb);
    }

    private void assertSingleSD(String mode) {
        final List<String> sds = Arrays.asList("type1");
        VespaModel model = new VespaModelCreatorWithMockPkg(vespaHosts, createVespaServices(sds, mode),
                ApplicationPackageUtils.generateSearchDefinitions(sds)).create();
        IndexedSearchCluster indexedSearchCluster = (IndexedSearchCluster)model.getSearchClusters().get(0);
        ContentSearchCluster contentSearchCluster = model.getContentClusters().get("test").getSearch();
        assertEquals(1, indexedSearchCluster.getDocumentDbs().size());
        String type1Id = "test/search/cluster.test/type1";
        ProtonConfig proton = getProtonCfg(contentSearchCluster);
        assertEquals(1, proton.documentdb().size());
        assertEquals("type1", proton.documentdb(0).inputdoctypename());
        assertEquals(type1Id, proton.documentdb(0).configid());
    }
    @Test
    public void requireThatWeCanHaveOneSDForIndexedMode() throws IOException, SAXException, ParseException {
        assertSingleSD("index");
    }

    private void assertDocTypeConfig(VespaModel model, String configId, String indexField, String attributeField) {
        IndexschemaConfig icfg = model.getConfig(IndexschemaConfig.class, configId);
        assertEquals(1, icfg.indexfield().size());
        assertEquals(indexField, icfg.indexfield(0).name());
        AttributesConfig acfg = model.getConfig(AttributesConfig.class, configId);
        assertEquals(2, acfg.attribute().size());
        assertEquals(attributeField, acfg.attribute(0).name());
        assertEquals(attributeField+"_nfa", acfg.attribute(1).name());
        RankProfilesConfig rcfg = model.getConfig(RankProfilesConfig.class, configId);
        assertEquals(6, rcfg.rankprofile().size());
    }

    @Test
    public void requireThatWeCanHaveMultipleSearchDefinitions() throws IOException, SAXException, ParseException {
        final List<String> sds = Arrays.asList("type1", "type2", "type3");
        VespaModel model = new VespaModelCreatorWithMockPkg(vespaHosts, createVespaServices(sds, "index"),
                ApplicationPackageUtils.generateSearchDefinitions(sds)).create();
        IndexedSearchCluster indexedSearchCluster = (IndexedSearchCluster)model.getSearchClusters().get(0);
        ContentSearchCluster contentSearchCluster = model.getContentClusters().get("test").getSearch();
        String type1Id = "test/search/cluster.test/type1";
        String type2Id = "test/search/cluster.test/type2";
        String type3Id = "test/search/cluster.test/type3";
        {
            assertEquals(3, indexedSearchCluster.getDocumentDbs().size());
            ProtonConfig proton = getProtonCfg(contentSearchCluster);
            assertEquals(3, proton.documentdb().size());
            assertEquals("type1", proton.documentdb(0).inputdoctypename());
            assertEquals(type1Id, proton.documentdb(0).configid());
            assertEquals("type2", proton.documentdb(1).inputdoctypename());
            assertEquals(type2Id, proton.documentdb(1).configid());
            assertEquals("type3", proton.documentdb(2).inputdoctypename());
            assertEquals(type3Id, proton.documentdb(2).configid());
        }
        assertDocTypeConfig(model, type1Id, "f1", "f2");
        assertDocTypeConfig(model, type2Id, "f3", "f4");
        assertDocTypeConfig(model, type3Id, "f5", "f6");
        {
            IndexInfoConfig iicfg = model.getConfig(IndexInfoConfig.class, "test/search/cluster.test");
            assertEquals(3, iicfg.indexinfo().size());
            assertEquals("type1", iicfg.indexinfo().get(0).name());
            assertEquals("type2", iicfg.indexinfo().get(1).name());
            assertEquals("type3", iicfg.indexinfo().get(2).name());
        }
        {
            AttributesConfig rac1 = model.getConfig(AttributesConfig.class, type1Id);
            assertEquals(2, rac1.attribute().size());
            assertEquals("f2", rac1.attribute(0).name());
            assertEquals("f2_nfa", rac1.attribute(1).name());
            AttributesConfig rac2 = model.getConfig(AttributesConfig.class, type2Id);
            assertEquals(2, rac2.attribute().size());
            assertEquals("f4", rac2.attribute(0).name());
            assertEquals("f4_nfa", rac2.attribute(1).name());
        }
        {
            IlscriptsConfig icfg = model.getConfig(IlscriptsConfig.class, "test/search/cluster.test");
            assertEquals(3, icfg.ilscript().size());
            assertEquals("type1", icfg.ilscript(0).doctype());
            assertEquals("type2", icfg.ilscript(1).doctype());
            assertEquals("type3", icfg.ilscript(2).doctype());
        }
    }

    @Test
    public void requireThatRelevantConfigIsAvailableForClusterSearcher() throws ParseException, IOException, SAXException {
        final List<String> sds = Arrays.asList("type1", "type2");
        VespaModel model = new VespaModelCreatorWithMockPkg(vespaHosts, createVespaServices(sds, "index"),
                ApplicationPackageUtils.generateSearchDefinitions(sds)).create();
        String searcherId = "container/searchchains/chain/test/component/com.yahoo.prelude.cluster.ClusterSearcher";

        { // documentdb-info config
            DocumentdbInfoConfig dcfg = model.getConfig(DocumentdbInfoConfig.class, searcherId);
            assertEquals(2, dcfg.documentdb().size());

            { // type1
                DocumentdbInfoConfig.Documentdb db = dcfg.documentdb(0);
                assertEquals("type1", db.name());
                assertEquals(6, db.rankprofile().size());

                assertRankProfile(db, 0, "default", false, false);
                assertRankProfile(db, 1, "unranked", false, false);
                assertRankProfile(db, 2, "staticrank", false, false);
                assertRankProfile(db, 3, "summaryfeatures", true, false);
                assertRankProfile(db, 4, "inheritedsummaryfeatures", true, false);
                assertRankProfile(db, 5, "rankfeatures", false, true);


                assertEquals(2, db.summaryclass().size());
                assertEquals("default", db.summaryclass(0).name());
                assertEquals("attributeprefetch", db.summaryclass(1).name());
                assertSummaryField(db, 0, 0, "f1", "longstring", true);
                assertSummaryField(db, 0, 1, "f2", "integer", false);
            }
            { // type2
                DocumentdbInfoConfig.Documentdb db = dcfg.documentdb(1);
                assertEquals("type2", db.name());
            }
        }
        { // attributes config
            AttributesConfig acfg = model.getConfig(AttributesConfig.class, searcherId);
            assertEquals(4, acfg.attribute().size());
            assertEquals("f2", acfg.attribute(0).name());
            assertEquals("f2_nfa", acfg.attribute(1).name());
            assertEquals("f4", acfg.attribute(2).name());
            assertEquals("f4_nfa", acfg.attribute(3).name());

        }
    }

    private void assertRankProfile(DocumentdbInfoConfig.Documentdb db, int index, String name,
                                   boolean hasSummaryFeatures, boolean hasRankFeatures) {
        DocumentdbInfoConfig.Documentdb.Rankprofile rankProfile0 = db.rankprofile(index);
        assertEquals(name, rankProfile0.name());
        assertEquals(hasSummaryFeatures, rankProfile0.hasSummaryFeatures());
        assertEquals(hasRankFeatures, rankProfile0.hasRankFeatures());
    }

    private void assertSummaryField(DocumentdbInfoConfig.Documentdb db, int summaryClassIndex, int fieldIndex,
                                    String name, String type, boolean dynamic) {
        DocumentdbInfoConfig.Documentdb.Summaryclass.Fields field = db.summaryclass(summaryClassIndex).fields(fieldIndex);
        assertEquals(name, field.name());
        assertEquals(type, field.type());
        assertEquals(dynamic, field.dynamic());
    }

    private void assertDocumentDBConfigAvailableForStreaming(String mode) {
        final List<String> sds = Arrays.asList("type");
        VespaModel model = new VespaModelCreatorWithMockPkg(vespaHosts,  createVespaServices(sds, mode),
                ApplicationPackageUtils.generateSearchDefinitions(sds)).create();

        DocumentdbInfoConfig dcfg = model.getConfig(DocumentdbInfoConfig.class, "test/search/cluster.test.type");
        assertEquals(1, dcfg.documentdb().size());
        DocumentdbInfoConfig.Documentdb db = dcfg.documentdb(0);
        assertEquals("type", db.name());
    }

    @Test
    public void requireThatDocumentDBConfigIsAvailableForStreaming() throws ParseException, IOException, SAXException {
        assertDocumentDBConfigAvailableForStreaming("streaming");
    }


    private void assertAttributesConfigIndependentOfMode(String mode, List<String> sds,
                                                         List<String> documentDBConfigIds,
                                                         Map<String, List<String>> expectedAttributesMap) {
        VespaModel model = new VespaModelCreatorWithMockPkg(vespaHosts,  createVespaServices(sds, mode),
                ApplicationPackageUtils.generateSearchDefinitions(sds)).create();
        ContentSearchCluster contentSearchCluster = model.getContentClusters().get("test").getSearch();

        ProtonConfig proton = getProtonCfg(contentSearchCluster);
        assertEquals(sds.size(), proton.documentdb().size());
        for (int i = 0; i < sds.size(); i++) {
            assertEquals(sds.get(i), proton.documentdb(i).inputdoctypename());
            assertEquals(documentDBConfigIds.get(i), proton.documentdb(i).configid());
            List<String> expectedAttributes = expectedAttributesMap.get(sds.get(i));
            if (expectedAttributes != null) {
                AttributesConfig rac1 = model.getConfig(AttributesConfig.class, proton.documentdb(i).configid());
                assertEquals(expectedAttributes.size(), rac1.attribute().size());
                for (int j = 0; j < expectedAttributes.size(); j++) {
                    assertEquals(expectedAttributes.get(j), rac1.attribute(j).name());
                }
            }
        }
    }

    @Test
    public void testThatAttributesConfigIsProducedForIndexed() {
        assertAttributesConfigIndependentOfMode("index", Arrays.asList("type1"),
                Arrays.asList("test/search/cluster.test/type1"),
                ImmutableMap.of("type1", Arrays.asList("f2", "f2_nfa")));
    }
    @Test
    public void testThatAttributesConfigIsProducedForStreamingForFastAccessFields() {
        assertAttributesConfigIndependentOfMode("streaming", Arrays.asList("type1"),
                Arrays.asList("test/search/type1"),
                ImmutableMap.of("type1", Arrays.asList("f2")));
    }
    @Test
    public void testThatAttributesConfigIsNotProducedForStoreOnlyEvenForFastAccessFields() {
        assertAttributesConfigIndependentOfMode("store-only", Arrays.asList("type1"),
                Arrays.asList("test/search"), Collections.emptyMap());
    }

}
