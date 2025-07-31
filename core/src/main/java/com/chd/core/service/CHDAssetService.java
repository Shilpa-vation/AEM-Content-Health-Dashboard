package com.chd.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;

public class CHDAssetService {

    private static final long MAX_IMAGE_SIZE = 2_000_000;

    private int TOTAL_ASSET_COUNT;

    public Map<String, Object> analyzeAssets(ResourceResolver resolver, String DAM_ROOT_PATH, String ROOT_PATH) {
        TOTAL_ASSET_COUNT = 0;
        Map<String, Object> assetReport = new HashMap<>();
        Map<String, Map<String, Object>> assetIssueMap = new HashMap<>();
        Map<String, Integer> formatCounts = new HashMap<>();
        Set<String> seenNames = new HashSet<>();
        List<Asset> staleAssets = new ArrayList<>();

        Resource damRoot = resolver.getResource(DAM_ROOT_PATH);
        if (damRoot == null)
            return assetReport;

        traverseAssets(damRoot, assetIssueMap, formatCounts, seenNames, ROOT_PATH);

        List<Map<String, Object>> filteredIssues = new ArrayList<>();
        for (Map<String, Object> entry : assetIssueMap.values()) {
            List<String> messages = (List<String>) entry.get("messages");
            if (messages != null && !messages.isEmpty()) {
                filteredIssues.add(entry);
            }
        }
        assetReport.put("issues", filteredIssues);
        assetReport.put("issueCount", filteredIssues.size());
        assetReport.put("formatCounts", formatCounts);
        assetReport.put("totalAssets", TOTAL_ASSET_COUNT);
        assetReport.put("staleAssets", staleAssets.size());

        return assetReport;
    }

    private void traverseAssets(Resource resource,
            Map<String, Map<String, Object>> assetIssueMap,
            Map<String, Integer> formatCounts,
            Set<String> seenNames,
            String ROOT_PATH) {

        for (Resource child : resource.getChildren()) {
            Asset asset = child.adaptTo(Asset.class);
            if (asset != null) {
                String name = asset.getName();
                String path = asset.getPath();
                String mimeType = asset.getMimeType();
                Rendition original = asset.getRendition("original");
                String alt = asset.getMetadataValue("dc:description");
                TOTAL_ASSET_COUNT++;
                // Set up issue entry if not already present
                Map<String, Object> issueEntry = assetIssueMap.computeIfAbsent(path, p -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("path", p);
                    entry.put("title", name);
                    entry.put("type", "assets");
                    entry.put("messages", new ArrayList<String>());
                    entry.put("status", "WARN"); // Can be upgraded to dynamic severity later
                    return entry;
                });

                List<String> messages = (List<String>) issueEntry.get("messages");

                // Check for large asset
                if (original != null) {
                    long size = original.getSize();
                    if (size > MAX_IMAGE_SIZE) {
                        messages.add("Large asset (" + (size / 1024) + " KB)");
                    }
                }

                // Format count
                String format = mimeType != null ? mimeType.substring(mimeType.lastIndexOf("/") + 1) : "unknown";
                formatCounts.merge(format.toLowerCase(), 1, Integer::sum);

                // Missing alt text
                if (StringUtils.isBlank(alt)) {
                    messages.add("Missing alt text");
                }

                // Duplicate asset name
                if (!seenNames.add(name)) {
                    messages.add("Duplicate asset name: " + name);
                }

                boolean isUsed = checkViaQueryBuilder(asset.getPath(), resource.getResourceResolver(), ROOT_PATH);
                if (!isUsed) {
                    messages.add("Unreferenced asset");
                }

            } else {
                // Recurse into folder
                traverseAssets(child, assetIssueMap, formatCounts, seenNames, ROOT_PATH);
            }
        }
    }

    private boolean checkViaQueryBuilder(String assetPath, ResourceResolver resolver, String ROOT_PATH) {
        Map<String, String> params = new HashMap<>();
        params.put("path", ROOT_PATH); // Broader reference search scope
        params.put("type", "nt:base");
        params.put("p.limit", "-1"); // No limit
        params.put("property", "fileReference");
        params.put("property.value", assetPath);

        // Include multiple properties to check
        List<String> refProps = Arrays.asList(
                "fileReference", "backgroundImage", "thumbnail", "imagePath",
                "cq:backgroundImage", "mediaRef");

        for (String prop : refProps) {
            params.put("property", prop);
            params.put("property.value", assetPath);

            com.day.cq.search.Query query = resolver.adaptTo(QueryBuilder.class)
                    .createQuery(PredicateGroup.create(params), resolver.adaptTo(Session.class));

            SearchResult result = query.getResult();
            if (result.getHits().size() > 0) {
                return true; // Found a reference
            }
        }

        return false; // No matches found in any checked property
    }

    public long getRecentPageCount(Session session, long recentPageCount, String ROOT_PATH) throws Exception {
        QueryManager qm = session.getWorkspace().getQueryManager();

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.HOUR, -24);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String dateStr = sdf.format(yesterday.getTime());

        String pageQueryStr = "SELECT * FROM [cq:PageContent] AS page " +
                "WHERE page.[jcr:created] >= CAST('" + dateStr + "' AS DATE) " +
                "AND ISDESCENDANTNODE(page, '" + ROOT_PATH + "')" +
                "AND page.[jcr:path] <> '" + ROOT_PATH + "/jcr:content'";

        Query pageQuery = qm.createQuery(pageQueryStr, Query.JCR_SQL2);
        QueryResult pageResult = pageQuery.execute();
        RowIterator pageRows = pageResult.getRows();
        return pageRows.getSize();
    }

    public long getRecentAssetCount(Session session, long recentAssetCount, String DAM_ROOT_PATH) throws Exception {
        QueryManager asqm = session.getWorkspace().getQueryManager();

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.HOUR, -24);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String dateStr = sdf.format(yesterday.getTime());

        String assetQueryStr = "SELECT * FROM [dam:Asset] AS asset " +
                "WHERE asset.[jcr:created] >= CAST('" + dateStr + "' AS DATE) " +
                "AND ISDESCENDANTNODE(asset, '" + DAM_ROOT_PATH + "')";

        Query assetQuery = asqm.createQuery(assetQueryStr, Query.JCR_SQL2);
        QueryResult assResult = assetQuery.execute();
        RowIterator assRows = assResult.getRows();
        long count = 0;
        while (assRows.hasNext()) {
            assRows.nextRow();
            count++;
        }
        return count;
    }

}