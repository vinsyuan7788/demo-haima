package com.demo.haima.test.unit.jmx.bean;

/**
 * @author Vince Yuan
 * @date 2021/11/18
 */
public class Student implements CustomMXBean {

    private Long id;

    private String name;

    public Student(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
