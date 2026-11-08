package com.cinx.commerce.voucher.api.mapper;

import com.cinx.commerce.voucher.api.dto.CreateVoucherRequest;
import com.cinx.commerce.voucher.api.dto.UpdateVoucherRequest;
import com.cinx.commerce.voucher.api.dto.VoucherPreviewResponse;
import com.cinx.commerce.voucher.api.dto.VoucherResponse;
import com.cinx.commerce.voucher.application.command.CreateVoucherCommand;
import com.cinx.commerce.voucher.application.command.UpdateVoucherCommand;
import com.cinx.commerce.voucher.application.query.VoucherPreview;
import com.cinx.commerce.voucher.application.query.VoucherView;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    CreateVoucherCommand toCommand(CreateVoucherRequest request);

    UpdateVoucherCommand toCommand(UpdateVoucherRequest request);

    VoucherResponse toResponse(VoucherView view);

    VoucherPreviewResponse toResponse(VoucherPreview preview);
}
