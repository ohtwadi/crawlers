/* Copyright 2023 Norconex Inc.
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
package com.norconex.crawler.core.pipeline.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.norconex.commons.lang.config.Configurable;
import com.norconex.commons.lang.text.TextMatcher;
import com.norconex.crawler.core.CoreStubber;
import com.norconex.crawler.core.doc.CrawlDocState;
import com.norconex.crawler.core.fetch.FetchDirective;
import com.norconex.crawler.core.fetch.FetchDirectiveSupport;
import com.norconex.crawler.core.filter.OnMatch;
import com.norconex.crawler.core.filter.impl.GenericMetadataFilter;

class MetadataFiltersStageTest {

    @Test
    void testMetadataFiltersStage(@TempDir Path tempDir) {
        var doc = CoreStubber.crawlDoc(
                "ref", "content", "myfield", "somevalue");
        var crawler = CoreStubber.crawler(tempDir);
        crawler.getConfiguration().setMetadataFetchSupport(
                FetchDirectiveSupport.REQUIRED);

        // Filter not matching
        crawler.getConfiguration().setMetadataFilters(List.of(
                Configurable.configure(new GenericMetadataFilter(), cfg -> cfg
                        .setFieldMatcher(TextMatcher.basic("blah"))
                        .setValueMatcher(TextMatcher.basic("blah"))
                        .setOnMatch(OnMatch.EXCLUDE))));
        var ctx = new ImporterPipelineContext(crawler, doc);
        doc.getDocRecord().setState(CrawlDocState.NEW);
        new MetadataFiltersStage(FetchDirective.METADATA).test(ctx);
        assertThat(doc.getDocRecord().getState()).isSameAs(CrawlDocState.NEW);

        // Filter matching
        crawler.getConfiguration().setMetadataFilters(List.of(
                Configurable.configure(new GenericMetadataFilter(), cfg -> cfg
                        .setFieldMatcher(TextMatcher.basic("myfield"))
                        .setValueMatcher(TextMatcher.basic("somevalue"))
                        .setOnMatch(OnMatch.EXCLUDE))));
        ctx = new ImporterPipelineContext(crawler, doc);
        doc.getDocRecord().setState(CrawlDocState.NEW);
        new MetadataFiltersStage(FetchDirective.METADATA).test(ctx);
        assertThat(doc.getDocRecord().getState()).isSameAs(
                CrawlDocState.REJECTED);
    }
}
