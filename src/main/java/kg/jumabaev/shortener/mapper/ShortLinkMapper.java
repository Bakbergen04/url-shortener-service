package kg.jumabaev.shortener.mapper;

import kg.jumabaev.shortener.dto.ShortLinkResponse;
import kg.jumabaev.shortener.entity.ShortLink;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ShortLinkMapper {

    ShortLinkResponse toResponse(ShortLink shortLink, String shortUrl);
}
