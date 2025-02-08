document.getElementById("input").addEventListener("submit", function (event) {
    event.preventDefault();

    let fileInput = document.getElementById("excelFile");
    let startDate = document.getElementById("startDate").value;
    let endDate = document.getElementById("endDate").value;

    if (!fileInput.files.length) {
        alert("Vui lòng chọn file Excel!");
        return;
    }
    if (!startDate || !endDate) {
        alert("Vui lòng nhập Start Date và End Date!");
        return;
    }

    let formData = new FormData();
    formData.append("excelFile", fileInput.files[0]);
    formData.append("startDate", startDate);
    formData.append("endDate", endDate);

    let loadingText = document.getElementById("loading");
    loadingText.style.display = "block";

    fetch("http://localhost:8080/api/process", {
        method: "POST",
        body: formData
    })
        .then(response => {
            if (!response.ok) {
                throw new Error("Lỗi server: " + response.status);
            }
            return response.json();
        })
        .then(data => {
            localStorage.setItem("processedData", JSON.stringify(data));
            window.location.href = "result.html";
        })
        .catch(error => {
            alert("Có lỗi xảy ra: " + error.message);
            console.error("Lỗi:", error);
        })
        .finally(() => {
            loadingText.style.display = "none"; // Ẩn loading dù có lỗi hay không
        });
});