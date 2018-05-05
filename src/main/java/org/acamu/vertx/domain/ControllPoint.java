package org.acamu.vertx.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.math.BigDecimal;

public class ControllPoint implements Serializable {

    private long id;
    private String content;
    private boolean validated;
    private BigDecimal price;

    @JsonCreator
    public ControllPoint(@JsonProperty(value = "id", required = true) long id,
                         @JsonProperty(value = "content", required = true) String content,
                         @JsonProperty(value = "validated", required = true) boolean validated,
                         @JsonProperty(value = "price", required = true) BigDecimal price) {
        this.id = id;
        this.content = content;
        this.validated = validated;
        this.price = price;
    }

    public ControllPoint(long id, BigDecimal price) {
        this.id = id;
        this.price = price;
    }

    public ControllPoint(long id) {
        this(id, BigDecimal.ZERO);
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    @Override
    public String toString() {
        return "{" +
                "\"" + "id:" + "\"" + id + "\"" +
                ", " + "\"" + "price:" + price + '\"' +
                ", " + "\"" + "validated:" + validated + '\"' +
                ", " + "\"" + "content:" + content + '\"' +
                "}";
    }
    /* default method
    private ControllPoint getControllPoint() {
        return this;
    }

    public String toJsonString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(getControllPoint());
            return json;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }*/
}
