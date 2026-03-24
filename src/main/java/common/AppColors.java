package common;

import java.awt.Color;

/**
 * Bảng màu Apothecary Pro — Design System cho toàn bộ app
 */
public class AppColors {

    // === PRIMARY — Xanh dương đậm (#0056B3) ===
    public static final Color PRIMARY           = new Color(0x00, 0x56, 0xB3);        // #0056B3
    public static final Color PRIMARY_DARK      = new Color(0x00, 0x3D, 0x80);        // #003D80
    public static final Color PRIMARY_LIGHT     = new Color(0x33, 0x78, 0xC2);        // #3378C2
    public static final Color PRIMARY_VERY_LIGHT = new Color(0xE3, 0xEF, 0xFA);       // #E3EFFA

    // === SECONDARY — Xám xanh (#64779E) ===
    public static final Color SECONDARY         = new Color(0x64, 0x77, 0x9E);        // #64779E
    public static final Color SECONDARY_DARK    = new Color(0x4A, 0x5A, 0x7A);        // #4A5A7A
    public static final Color SECONDARY_LIGHT   = new Color(0x8A, 0x9B, 0xB8);        // #8A9BB8

    // === TERTIARY — Đỏ cảnh báo (#D32F2F) ===
    public static final Color TERTIARY          = new Color(0xD3, 0x2F, 0x2F);        // #D32F2F
    public static final Color TERTIARY_LIGHT    = new Color(0xF8, 0xE0, 0xE0);        // #F8E0E0

    // === NEUTRAL — Nền sáng (#F8F9FA) ===
    public static final Color NEUTRAL           = new Color(0xF8, 0xF9, 0xFA);        // #F8F9FA
    public static final Color NEUTRAL_DARK      = new Color(0xE9, 0xEC, 0xEF);        // #E9ECEF
    public static final Color NEUTRAL_DARKER    = new Color(0xDE, 0xE2, 0xE6);        // #DEE2E6

    // === SIDEBAR ===
    public static final Color SIDEBAR_BG        = new Color(0x1B, 0x2A, 0x4A);        // Dark navy
    public static final Color SIDEBAR_TEXT       = new Color(0xBB, 0xC7, 0xDB);        // Light grey-blue
    public static final Color SIDEBAR_HOVER      = new Color(0x24, 0x3B, 0x63);        // Hover state
    public static final Color SIDEBAR_ACTIVE_BG  = PRIMARY;                            // Active = Primary
    public static final Color SIDEBAR_ACTIVE_TEXT = Color.WHITE;

    // === TEXT ===
    public static final Color TEXT_PRIMARY      = new Color(0x21, 0x25, 0x29);        // #212529
    public static final Color TEXT_SECONDARY    = new Color(0x6C, 0x75, 0x7D);        // #6C757D
    public static final Color TEXT_WHITE        = Color.WHITE;

    // === STATUS ===
    public static final Color SUCCESS           = new Color(0x28, 0xA7, 0x45);        // #28A745
    public static final Color WARNING           = new Color(0xFF, 0xC1, 0x07);        // #FFC107
    public static final Color DANGER            = TERTIARY;

    // === TABLE ===
    public static final Color TABLE_HEADER_BG   = PRIMARY;
    public static final Color TABLE_HEADER_FG   = Color.WHITE;
    public static final Color TABLE_ROW_ALT     = new Color(0xF0, 0xF4, 0xF8);        // Alternating row
    public static final Color TABLE_BORDER      = NEUTRAL_DARK;
}
