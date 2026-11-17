package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dao.NhaCungCapDao;
import com.hoangdinh.taphoa.dto.request.NhaCungCapRequest;
import com.hoangdinh.taphoa.dto.response.NhaCungCapResponse;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.Session;
import java.util.List;

/** Nhà cung cấp. Thu ngân xem được (để lập phiếu nhập); thêm / sửa / xóa chỉ chủ cửa hàng. */
public class NhaCungCapService {

    private final NhaCungCapDao nhaCungCapDao;

    public NhaCungCapService(NhaCungCapDao nhaCungCapDao) {
        this.nhaCungCapDao = nhaCungCapDao;
    }

    public List<NhaCungCapResponse> search(String keyword, boolean chiDangHopTac) {
        Session.requireLogin();
        return nhaCungCapDao.search(Jdbc.text(keyword), chiDangHopTac);
    }

    public NhaCungCapResponse create(NhaCungCapRequest req) {
        Session.requireAdmin();
        Validation.check(req);
        if (nhaCungCapDao.existsBySdt(req.getSdt().trim(), null)) {
            throw new BusinessException(ErrorCode.SDT_DA_TON_TAI);
        }
        return get(nhaCungCapDao.insert(req));
    }

    public NhaCungCapResponse update(int maNcc, NhaCungCapRequest req) {
        Session.requireAdmin();
        Validation.check(req);
        get(maNcc);
        if (nhaCungCapDao.existsBySdt(req.getSdt().trim(), maNcc)) {
            throw new BusinessException(ErrorCode.SDT_DA_TON_TAI);
        }
        nhaCungCapDao.update(maNcc, req);
        return get(maNcc);
    }

    /** Nhà cung cấp đã có phiếu nhập thì giữ lại để đối chiếu, chỉ chuyển sang ngừng hợp tác. */
    public void delete(int maNcc) {
        Session.requireAdmin();
        if (get(maNcc).getSoPhieuNhap() > 0) {
            throw new BusinessException(ErrorCode.NCC_DA_GIAO_DICH);
        }
        nhaCungCapDao.delete(maNcc);
    }

    NhaCungCapResponse get(int maNcc) {
        return nhaCungCapDao.findById(maNcc).orElseThrow(() -> new BusinessException(ErrorCode.NCC_NOT_FOUND));
    }
}
