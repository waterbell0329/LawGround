package com.lawground.global.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JsonConfiguration {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer strictApiJson() {
        return builder ->
                builder.featuresToEnable(
                                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS,
                                JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                        .featuresToDisable(
                                DeserializationFeature.ACCEPT_FLOAT_AS_INT,
                                MapperFeature.ALLOW_COERCION_OF_SCALARS)
                        .postConfigurer(
                                mapper -> {
                                    var textual = mapper.coercionConfigFor(LogicalType.Textual);
                                    for (var shape :
                                            new CoercionInputShape[] {
                                                CoercionInputShape.Integer,
                                                CoercionInputShape.Float,
                                                CoercionInputShape.Boolean
                                            }) textual.setCoercion(shape, CoercionAction.Fail);
                                })
                        .deserializerByType(
                                LocalDate.class,
                                new JsonDeserializer<LocalDate>() {
                                    @Override
                                    public LocalDate deserialize(
                                            JsonParser parser, DeserializationContext context)
                                            throws IOException {
                                        if (!parser.hasToken(JsonToken.VALUE_STRING)
                                                || !parser.getText()
                                                        .matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                                            throw JsonMappingException.from(
                                                    parser, "날짜는 YYYY-MM-DD 문자열이어야 합니다.");
                                        }
                                        try {
                                            return LocalDate.parse(parser.getText());
                                        } catch (DateTimeParseException invalid) {
                                            throw JsonMappingException.from(
                                                    parser, "유효한 날짜를 입력하세요.");
                                        }
                                    }
                                });
    }
}
