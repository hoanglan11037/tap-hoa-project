package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.dao.DanhMucDao;
import com.hoangdinh.taphoa.dto.request.DanhMucRequest;
import com.hoangdinh.taphoa.dto.response.DanhMucResponse;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.Session;
import java.util.List;

/** Danh mục (nhóm hàng). Ai cũng xem được để lọc sản phẩm; thêm / sửa / xóa chỉ chủ cửa hàng. */
public class DanhMucService {

    private final DanhMucDao danhMucDao;

    public DanhMucService(DanhMucDao danhMucDao) {
        this.danhMucDao = danhMucDao;
    }

    public List<DanhMucResponse> findAll() {
        Session.requireLogin();
        return danhMucDao.findAll();
    }

    public DanhMucResponse create(DanhMucRequest req) {
        Session.requireAdmin();
        Validation.check(req);
        if (danhMucDao.existsByTen(req.getTenDm().trim(), null)) {
            throw new BusinessException(ErrorCode.TEN_DANH_MUC_DA_TON_TAI);
        }
        return get(danhMucDao.insert(req));
    }

    public DanhMucResponse update(int maDm, DanhMucRequest req) {
        Session.requireAdmin();
        Validation.check(req);
        get(maDm);
        if (danhMucDao.existsByTen(req.getTenDm().trim(), maDm)) {
            throw new BusinessException(ErrorCode.TEN_DANH_MUC_DA_TON_TAI);
        }
        danhMucDao.update(maDm, req);
        return get(maDm);
    }

    /** Chỉ xóa được danh mục rỗng. */
    public void delete(int maDm) {
        Session.requireAdmin();
        if (get(maDm).getSoSanPham() > 0) {
            throw new BusinessException(ErrorCode.DANH_MUC_CON_SAN_PHAM);
        }
        danhMucDao.delete(maDm);
    }

    private DanhMucResponse get(int maDm) {
        return danhMucDao.findById(maDm).orElseThrow(() -> new BusinessException(ErrorCode.DANH_MUC_NOT_FOUND));
    }
}
