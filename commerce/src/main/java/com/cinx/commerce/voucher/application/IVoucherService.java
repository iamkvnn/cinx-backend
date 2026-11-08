package com.cinx.commerce.voucher.application;

import com.cinx.commerce.voucher.application.command.CreateVoucherCommand;
import com.cinx.commerce.voucher.application.command.UpdateVoucherCommand;
import com.cinx.commerce.voucher.application.query.VoucherPreview;
import com.cinx.commerce.voucher.application.query.VoucherView;
import com.cinx.common.paging.PageQuery;
import com.cinx.common.paging.PageResult;

public interface IVoucherService {
    VoucherView create(CreateVoucherCommand command);

    PageResult<VoucherView> findAll(PageQuery page);

    VoucherView findByCode(String code);

    VoucherPreview preview(String userId, String code);

    VoucherView update(String code, UpdateVoucherCommand command);

    VoucherView setActive(String code, boolean active);
}
