package com.cosx.knowengine.document.vector;

import com.cosx.knowengine.document.support.Sha256Utils;
import com.cosx.knowengine.document.vector.config.ElasticsearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VectorFingerprint {

    private final DocumentEmbeddingModel embeddingModel;
    private final ElasticsearchProperties elasticsearchProperties;

    public String calculate(String content) {
        return Sha256Utils.digest(normalize(content) + "\n"
                + embeddingModel.modelKey() + "\n"
                + embeddingModel.dimensions() + "\n"
                + elasticsearchProperties.getIndexSchemaVersion());
    }

    private String normalize(String content) {
        return content == null ? "" : content.replace("\r\n", "\n").strip();
    }
}
