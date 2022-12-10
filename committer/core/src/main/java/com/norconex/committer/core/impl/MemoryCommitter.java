/* Copyright 2019-2022 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.committer.core.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.committer.core.AbstractCommitter;
import com.norconex.committer.core.CommitterException;
import com.norconex.committer.core.CommitterRequest;
import com.norconex.committer.core.DeleteRequest;
import com.norconex.committer.core.UpsertRequest;
import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.commons.lang.xml.XML;

import lombok.EqualsAndHashCode;

/**
 * <p>
 * <b>WARNING: Not intended for production use.</b>
 * </p>
 * <p>
 * A Committer that stores every document received into memory.
 * This can be useful for testing or troubleshooting applications using
 * Committers.
 * Given this committer can eat up memory pretty quickly, its use is strongly
 * discouraged for regular production use.
 * </p>
 *
 * {@nx.xml.usage
 * <committer class="com.norconex.committer.core.impl.MemoryCommitter">
 *   {@nx.include com.norconex.committer.core.AbstractCommitter@nx.xml.usage}
 * </committer>
 * }
 *
 */
@SuppressWarnings("javadoc")
@EqualsAndHashCode(callSuper = true)
public class MemoryCommitter extends AbstractCommitter {

    private static final Logger LOG =
            LoggerFactory.getLogger(MemoryCommitter.class);

    private final List<CommitterRequest> requests = new ArrayList<>();

    private int upsertCount = 0;
    private int deleteCount = 0;

    private boolean ignoreContent;
    private final TextMatcher fieldMatcher = new TextMatcher();

    public boolean isIgnoreContent() {
        return ignoreContent;
    }
    public void setIgnoreContent(boolean ignoreContent) {
        this.ignoreContent = ignoreContent;
    }

    public TextMatcher getFieldMatcher() {
        return fieldMatcher;
    }
    public void setFieldMatcher(TextMatcher fieldMatcher) {
        this.fieldMatcher.copyFrom(fieldMatcher);
    }

    @Override
    protected void doInit() {
        //NOOP
    }

    public boolean removeRequest(CommitterRequest req) {
        return requests.remove(req);
    }

    public List<CommitterRequest> getAllRequests() {
        return requests;
    }
    public List<UpsertRequest> getUpsertRequests() {
        return requests.stream()
                .filter(UpsertRequest.class::isInstance)
                .map(UpsertRequest.class::cast)
                .toList();
    }
    public List<DeleteRequest> getDeleteRequests() {
        return requests.stream()
                .filter(DeleteRequest.class::isInstance)
                .map(DeleteRequest.class::cast)
                .toList();
    }

    public int getUpsertCount() {
        return upsertCount;
    }
    public int getDeleteCount() {
        return deleteCount;
    }
    public int getRequestCount() {
        return requests.size();
    }

    @Override
    protected void doUpsert(UpsertRequest upsertRequest)
            throws CommitterException {
        var memReference = upsertRequest.getReference();
        LOG.debug("Committing upsert request for {}", memReference);

        InputStream memContent = null;
        var reqContent = upsertRequest.getContent();
        if (!ignoreContent && reqContent != null) {
            try {
                memContent = new ByteArrayInputStream(
                        IOUtils.toByteArray(reqContent));
            } catch (IOException e) {
                throw new CommitterException(
                        "Could not do upsert for " + memReference);
            }
        }

        var memMetadata = filteredMetadata(upsertRequest.getMetadata());

        requests.add(new UpsertRequest(memReference, memMetadata, memContent));
        upsertCount++;
    }
    @Override
    protected void doDelete(DeleteRequest deleteRequest) {
        var memReference = deleteRequest.getReference();
        LOG.debug("Committing delete request for {}", memReference);
        var memMetadata = filteredMetadata(deleteRequest.getMetadata());
        requests.add(new DeleteRequest(memReference, memMetadata));
        deleteCount++;
    }

    private Properties filteredMetadata(Properties reqMetadata) {
        var memMetadata = new Properties();
        if (reqMetadata != null) {

            if (fieldMatcher.getPattern() == null) {
                memMetadata.loadFromMap(reqMetadata);
            } else {
                memMetadata.loadFromMap(reqMetadata.entrySet().stream()
                        .filter(en -> fieldMatcher.matches(en.getKey()))
                        .collect(Collectors.toMap(
                                Entry::getKey, Entry::getValue)));
            }
        }
        return memMetadata;
    }

    @Override
    protected void doClose()
            throws com.norconex.committer.core.CommitterException {
        LOG.info("{} upserts committed.", upsertCount);
        LOG.info("{} deletions committed.", deleteCount);
    }

    @Override
    protected void doClean() throws CommitterException {
        requests.clear();
        upsertCount = 0;
        deleteCount = 0;
    }

    @Override
    public String toString() {
        // Cannot use ReflectionToStringBuilder here to prevent
        // "An illegal reflective access operation has occurred"
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("requests", requests, false)
                .append("upsertCount", upsertCount)
                .append("deleteCount", deleteCount)
                .build();
    }

    @Override
    public void loadCommitterFromXML(XML xml) {
        //NOOP
    }

    @Override
    public void saveCommitterToXML(XML xml) {
        //NOOP
    }
}
