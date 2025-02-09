# Toyota Transportation Planning

**Toyota Transportation Planning** là một API xây dựng bằng Spring Boot giúp xử lý dữ liệu và tối ưu lập lịch vận chuyển đơn hàng từ kho đến các đại lý cho doanh nghiệp Toyota. 

API cho phép người dùng tải lên file Excel, xử lý dữ liệu và tối ưu giá thành vận chuyển sử dụng thư viện **Google OrTools** giải bài toán **quy hoạch hỗn hợp nguyên-tuyến tính**

Kết quả sẽ được hiển thị trên giao diện web và có thể tải xuống file Excel.

## Tính năng
- Nhận file Excel từ người dùng.
- Đọc dữ liệu từ file Excel
- Giải bài toán tối ưu giá thành sử dụng thư viện Google OrTools
- Hiển thị kết quả xử lý trên giao diện web.
- Xuất file Excel mới chứa dữ liệu đã xử lý.

## Cài đặt & Chạy API

### Yêu cầu hệ thống: 
JDK 17+. Nếu chưa có, tải và cài đặt OpenJDK từ [https://adoptium.net/](https://adoptium.net/)  

### Hướng dẫn chạy
1. Clone repository:
   
```bash
   git clone https://github.com/ThanhIT0410/Toyota-Transportation-Planning.git
   cd toyota-transportation-planning
```
2. Chạy JAR:
```bash
   java -jar target/ToyotaTransportationPlanning-0.0.1-SNAPSHOT.jar
```
Sau khi chạy, API sẽ khởi động trên http://localhost:8080

3. Sử dụng API:
   - Tải file Sample.xlsx đính kèm trong repo lên
   - Chọn ngày bắt đầu và ngày kết thúc cho khoảng thời gian cần lập lịch vận chuyển (chọn trong khoảng 01/11/2024 đến 30/11/2024 đối với file Sample)
   - Nhấn "Tối ưu" để bắt đầu xử lý dữ liệu
