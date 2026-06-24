package kg.bakbergen.shortener.mapper;

import kg.bakbergen.shortener.dto.ShortLinkResponse;
import kg.bakbergen.shortener.entity.ShortLink;
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
