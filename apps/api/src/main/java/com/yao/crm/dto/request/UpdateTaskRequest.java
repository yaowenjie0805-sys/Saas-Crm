package com.yao.crm.dto.request;

import javax.validation.constraints.Size;

public class UpdateTaskRequest {

    @Size(max = 200, message = "title_too_long")
    private String title;

    private String time;

    @Size(max = 20, message = "level_too_long")
    private String level;

    private Boolean done;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }
}