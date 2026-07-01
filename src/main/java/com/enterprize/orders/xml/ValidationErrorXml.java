package com.enterprize.orders.xml;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "validationErrors")
@XmlAccessorType(XmlAccessType.FIELD)
public class ValidationErrorXml {

    @XmlElement(name = "error")
    private List<String> errors = new ArrayList<>();

    public ValidationErrorXml() {
    }

    public ValidationErrorXml(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
