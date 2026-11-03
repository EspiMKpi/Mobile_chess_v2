// Build gốc: chỉ khai báo plugin Android, từng module tự cấu hình.
plugins {
    alias(libs.plugins.android.application) apply false
}
