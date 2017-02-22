// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui.servertree;

import com.intellij.openapi.util.Condition;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.rest.VersionControlRecursionTypeCaseSensitive;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcItem;
import com.microsoft.alm.sourcecontrol.webapi.model.TfvcVersionDescriptor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TfsTreeContext {
    public static Logger logger = LoggerFactory.getLogger(TfsTreeContext.class);

    public final ServerContext serverContext;
    private final boolean foldersOnly;
    @Nullable
    private final Condition<String> filter;

    public TfsTreeContext(final ServerContext serverContext, final boolean foldersOnly, final Condition<String> filter) {
        this.serverContext = serverContext;
        this.foldersOnly = foldersOnly;
        this.filter = filter;
    }

    public boolean isAccepted(final String path) {
        return filter == null || filter.value(path);
    }

    public List<TfvcItem> getChildItems(final String path) throws TfsException {
        if (serverContext.getTeamProjectReference() == null) {
            logger.warn("Project cannot be determined so can't get server tree");
            return Collections.emptyList();
        }

        final List<TfvcItem> items = serverContext.getTfvcHttpClient().getItems(serverContext.getTeamProjectReference().getId(),
                path, VersionControlRecursionTypeCaseSensitive.ONE_LEVEL, new TfvcVersionDescriptor());

        // API returns the parent along with its children so remove the parent from the list
        TfvcItem parentItem = null;
        for (final TfvcItem item : items) {
            if (StringUtils.equals(item.getPath(), path)) {
                logger.info("Parent item found and being removed from children list");
                parentItem = item;
                break;
            }
        }
        items.remove(parentItem);

        // if only folders needed then filter them out of the list else just return
        if (foldersOnly) {
            logger.info("Filter out children for only folders");
            final List<TfvcItem> folderItems = new ArrayList<TfvcItem>(items.size());
            for (final TfvcItem item : items) {
                if (item.isFolder()) {
                    folderItems.add(item);
                }
            }
            return folderItems;
        } else {
            return items;
        }
    }
}