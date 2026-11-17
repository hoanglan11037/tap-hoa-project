package com.hoangdinh.taphoa.service;

import com.hoangdinh.taphoa.config.Database;
import com.hoangdinh.taphoa.config.Jdbc;
import com.hoangdinh.taphoa.dao.NhaCungCapDao;
import com.hoangdinh.taphoa.dao.PhieuNhapDao;
import com.hoangdinh.taphoa.dao.SanPhamDao;
import com.hoangdinh.taphoa.dto.request.DongNhapRequest;
import com.hoangdinh.taphoa.dto.request.PhieuNhapRequest;
import com.hoangdinh.taphoa.dto.response.NhaCungCapResponse;
import com.hoangdinh.taphoa.dto.response.PhieuNhapResponse;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.hoangdinh.taphoa.security.Session;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Nhập hàng từ nhà cung cấp: tăng tồn kho và cập nhật giá nhập mới nhất của sản phẩm. */
public class NhapHangService {

    private static final int SO_PHIEU_GAN_DAY = 100;

    private final PhieuNhapDao phieuNhapDao;
    private final NhaCungCapDao nhaCungCapDao;
    private final SanPhamDao sanPhamDao;

    public NhapHangService(PhieuNhapDao phieuNhapDao, NhaCungCapDao nhaCungCapDao, SanPhamDao sanPhamDao) {
        this.phieuNhapDao = phieuNhapDao;
        this.nhaCungCapDao = nhaCungCapDao;
        this.sanPhamDao = sanPhamDao;
    }

    public List<PhieuNhapResponse> findAll() {
        Session.requireLogin();
        return phieuNhapDao.findMoiNhat(SO_PHIEU_GAN_DAY);
    }

    public PhieuNhapResponse chiTiet(int maPn) {
        Session.requireLogin();
        PhieuNhapResponse pn = phieuNhapDao.findById(maPn)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHIEU_NHAP_NOT_FOUND));
        pn.setChiTiet(phieuNhapDao.findChiTiet(maPn));
        return pn;
    }

    /** Lập phiếu nhập và cộng kho trong một giao dịch. */
    public PhieuNhapResponse create(PhieuNhapRequest req) {
        int maTk = Session.current().getMaTk();
        Validation.check(req);
        if (req.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.PHIEU_NHAP_TRONG);
        }
        Set<Integer> daCo = new HashSet<>();
        for (DongNhapRequest d : req.getItems()) {
            if (!daCo.add(d.getMaSp())) {
                throw new BusinessException(ErrorCode.SAN_PHAM_LAP_LAI);
            }
        }
        return Database.inTransaction(() -> {
            NhaCungCapResponse ncc = nhaCungCapDao.findById(req.getMaNcc())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NCC_NOT_FOUND));
            if (!ncc.isTrangThai()) {
                throw new BusinessException(ErrorCode.NCC_NGUNG_HOP_TAC);
            }
            int maPn = phieuNhapDao.insert(ncc.getMaNcc(), maTk, Jdbc.text(req.getGhiChu()));
            for (DongNhapRequest d : req.getItems()) {
                sanPhamDao.findById(d.getMaSp()).orElseThrow(() -> new BusinessException(ErrorCode.SAN_PHAM_NOT_FOUND));
                phieuNhapDao.insertChiTiet(maPn, d.getMaSp(), d.getSoLuong(), d.getDonGia());
                sanPhamDao.nhapKho(d.getMaSp(), d.getSoLuong(), d.getDonGia());
            }
            return chiTiet(maPn);
        });
    }
}
