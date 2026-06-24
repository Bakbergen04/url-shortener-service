package kg.bakbergen.shortener.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import kg.bakbergen.shortener.util.ValidHttpUrl;
import lombok.Getter;

import java.time.Instant;

@Getter
public class UpdateShortLinkRequest {

    @ValidHttpUrl(allowNull = true)
    private String originalUrl;

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private Boolean active;

    @Future(message = "expiresAt must be in the future")
    private Instant expiresAt;

    @JsonIgnore
    private boolean originalUrlPresent;

    @JsonIgnore
    private boolean titlePresent;

    @JsonIgnore
    private boolean activePresent;

    @JsonIgnore
    private boolean expiresAtPresent;

    @JsonSetter("originalUrl")
    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
        this.originalUrlPresent = true;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    @JsonSetter("active")
    public void setActive(Boolean active) {
        this.active = active;
        this.activePresent = true;
    }

    @JsonSetter("expiresAt")
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        this.expiresAtPresent = true;
    }

    @AssertTrue(message = "at least one field must be provided")
    @JsonIgnore
    public boolean isAnyFieldPresent() {
        return originalUrlPresent || titlePresent || activePresent || expiresAtPresent;
    }

    @AssertTrue(message = "originalUrl must not be null when provided")
    @JsonIgnore
    public boolean isOriginalUrlPresentWithValue() {
        return !originalUrlPresent || originalUrl != null;
    }

    @AssertTrue(message = "active must not be null when provided")
    @JsonIgnore
    public boolean isActivePresentWithValue() {
        return !activePresent || active != null;
    }
}
