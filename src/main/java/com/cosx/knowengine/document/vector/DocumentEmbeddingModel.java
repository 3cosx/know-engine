package com.cosx.knowengine.document.vector;

import java.util.List;

public interface DocumentEmbeddingModel {

    List<float[]> embedAll(List<String> contents);

    String modelKey();

    int dimensions();
}
