package com.reviewflow.model.dto.response;

import lombok.Builder;
import lombok.Value;
import io.swagger.v3.oas.annotations.media.Schema;

@Value
@Builder
public class PreviewResponseDto {

    String previewUrl;
    String contentType;
    Long expiresInSeconds;
    String filename;
}
