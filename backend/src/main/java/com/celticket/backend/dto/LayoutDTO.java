package com.celticket.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LayoutDTO {
    private Map<String, Object> meta;
    private List<SectionDTO> sections;
    private List<Object> seats;
    private List<Object> labels;
    private List<Map<String, Object>> tarimas;
    private Map<String, Object> tarima;

    public LayoutDTO() {}

    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
    public List<SectionDTO> getSections() { return sections; }
    public void setSections(List<SectionDTO> sections) { this.sections = sections; }
    public List<Object> getSeats() { return seats; }
    public void setSeats(List<Object> seats) { this.seats = seats; }
    public List<Object> getLabels() { return labels; }
    public void setLabels(List<Object> labels) { this.labels = labels; }
    public List<Map<String, Object>> getTarimas() { return tarimas; }
    public void setTarimas(List<Map<String, Object>> tarimas) { this.tarimas = tarimas; }
    public Map<String, Object> getTarima() { return tarima; }
    public void setTarima(Map<String, Object> tarima) { this.tarima = tarima; }
}
