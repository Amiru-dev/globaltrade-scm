package com.globaltrade.common.dto;

import com.globaltrade.common.entity.CarrierRoute;
import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.entity.Vendor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized DTO conversion utility for entity-to-DTO transformations.
 *
 * <p><strong>EJB Best Practice - DTO Conversion:</strong></p>
 * <ul>
 *   <li><strong>Centralized Logic:</strong> All conversion logic in one place for maintainability</li>
 *   <li><strong>Consistency:</strong> Ensures consistent conversion patterns across the application</li>
 *   <li><strong>Performance:</strong> Stream-based conversion for collections</li>
 *   <li><strong>Type Safety:</strong> Generic methods for type-safe conversions</li>
 * </ul>
 */
public class DTOConverter {

    /**
     * Converts a single entity to DTO.
     *
     * @param entity    the entity to convert
     * @param converter the conversion function
     * @return the converted DTO, or null if entity is null
     */
    public static <E, D> D convertToDTO(E entity, EntityToDTOConverter<E, D> converter) {
        if (entity == null) {
            return null;
        }
        return converter.convert(entity);
    }

    /**
     * Converts a list of entities to DTOs.
     *
     * @param entities  the list of entities to convert
     * @param converter the conversion function
     * @return the list of converted DTOs
     */
    public static <E, D> List<D> convertToDTOList(List<E> entities, EntityToDTOConverter<E, D> converter) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(converter::convert)
                .collect(Collectors.toList());
    }

    /**
     * Converts a single DTO to entity.
     *
     * @param dto       the DTO to convert
     * @param converter the conversion function
     * @return the converted entity, or null if DTO is null
     */
    public static <D, E> E convertToEntity(D dto, DTOToEntityConverter<D, E> converter) {
        if (dto == null) {
            return null;
        }
        return converter.convert(dto);
    }

    /**
     * Converts a list of DTOs to entities.
     *
     * @param dtos      the list of DTOs to convert
     * @param converter the conversion function
     * @return the list of converted entities
     */
    public static <D, E> List<E> convertToEntityList(List<D> dtos, DTOToEntityConverter<D, E> converter) {
        if (dtos == null) {
            return null;
        }
        return dtos.stream()
                .map(converter::convert)
                .collect(Collectors.toList());
    }

    // ======================== Specific Entity Conversions ========================

    /**
     * Converts a Shipment entity to ShipmentDTO.
     */
    public static ShipmentDTO toShipmentDTO(Shipment entity) {
        return ShipmentDTO.fromEntity(entity);
    }

    /**
     * Converts a list of Shipment entities to ShipmentDTOs.
     */
    public static List<ShipmentDTO> toShipmentDTOList(List<Shipment> entities) {
        return convertToDTOList(entities, ShipmentDTO::fromEntity);
    }

    /**
     * Converts a ShipmentDTO to Shipment entity.
     */
    public static Shipment toShipmentEntity(ShipmentDTO dto) {
        return dto.toEntity();
    }

    /**
     * Converts an InventoryItem entity to InventoryItemDTO.
     */
    public static InventoryItemDTO toInventoryItemDTO(InventoryItem entity) {
        return InventoryItemDTO.fromEntity(entity);
    }

    /**
     * Converts a list of InventoryItem entities to InventoryItemDTOs.
     */
    public static List<InventoryItemDTO> toInventoryItemDTOList(List<InventoryItem> entities) {
        return convertToDTOList(entities, InventoryItemDTO::fromEntity);
    }

    /**
     * Converts an InventoryItemDTO to InventoryItem entity.
     */
    public static InventoryItem toInventoryItemEntity(InventoryItemDTO dto) {
        return dto.toEntity();
    }

    /**
     * Converts a Vendor entity to VendorDTO.
     */
    public static VendorDTO toVendorDTO(Vendor entity) {
        return VendorDTO.fromEntity(entity);
    }

    /**
     * Converts a list of Vendor entities to VendorDTOs.
     */
    public static List<VendorDTO> toVendorDTOList(List<Vendor> entities) {
        return convertToDTOList(entities, VendorDTO::fromEntity);
    }

    /**
     * Converts a VendorDTO to Vendor entity.
     */
    public static Vendor toVendorEntity(VendorDTO dto) {
        return dto.toEntity();
    }

    /**
     * Converts a CarrierRoute entity to CarrierRouteDTO.
     */
    public static CarrierRouteDTO toCarrierRouteDTO(CarrierRoute entity) {
        return CarrierRouteDTO.fromEntity(entity);
    }

    /**
     * Converts a list of CarrierRoute entities to CarrierRouteDTOs.
     */
    public static List<CarrierRouteDTO> toCarrierRouteDTOList(List<CarrierRoute> entities) {
        return convertToDTOList(entities, CarrierRouteDTO::fromEntity);
    }

    /**
     * Converts a CarrierRouteDTO to CarrierRoute entity.
     */
    public static CarrierRoute toCarrierRouteEntity(CarrierRouteDTO dto) {
        return dto.toEntity();
    }

    // ======================== Functional Interfaces ========================

    /**
     * Functional interface for entity-to-DTO conversion.
     */
    @FunctionalInterface
    public interface EntityToDTOConverter<E, D> {
        D convert(E entity);
    }

    /**
     * Functional interface for DTO-to-entity conversion.
     */
    @FunctionalInterface
    public interface DTOToEntityConverter<D, E> {
        E convert(D dto);
    }
}