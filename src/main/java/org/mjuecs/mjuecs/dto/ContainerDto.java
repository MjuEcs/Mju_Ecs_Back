package org.mjuecs.mjuecs.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ContainerDto {
    private String imageName;
    private int hostPort;
    private Map<String, String> env;
    private List<String> cmd;
}
