document.addEventListener("DOMContentLoaded", function () {
    let data = JSON.parse(localStorage.getItem("processedData"));

    let resultTable = document.getElementById("resultTable");
    let deliveryTable = document.getElementById("deliveryTable");
    let resultTableBody = document.getElementById("resultTable-body");
    let deliveryTableBody = document.getElementById("deliveryTable-body");

    let maxVehicleID = Math.max(...data.orders.map(order => order.assignedVehicleID || 0));

    // Result
    data.orders.forEach((order, index) => {
        let row = `
            <tr>
                <td>${index + 1}</td>
                <td>${order.grade}</td>
                <td>${order.id}</td>
                <td>${order.date}</td>
                <td>${order.source}</td>
                <td>${order.destination}</td>
                <td>${order.assignedVehicleID !== null ? order.assignedVehicleID : "Không có chuyến phù hợp"}</td>
                <td>${order.assignedVehicle || ""}</td>
                <td>${order.assignedUnit || ""}</td>
            </tr>
        `;
        resultTableBody.innerHTML += row;
    });

    document.getElementById("totalPrice").innerText = data.totalPrice;
    document.getElementById("carCount").innerText = data.carCount;

    // Delivery
    for (let vehicleID = 0; vehicleID <= maxVehicleID; vehicleID++) {
        let vehicleOrders = [];

        data.orders.forEach((order, index) => {
            if (order.assignedVehicleID !== null && order.assignedVehicleID !== undefined && order.assignedVehicleID === vehicleID) {
                vehicleOrders.push({ index: index + 1, id: order.id });
            }
        });

        if (vehicleOrders.length > 0) {
            let firstOrder = data.orders.find(order => order.assignedVehicleID === vehicleID);
            let row = `
                <tr>
                    <td>${vehicleID}</td>
                    <td>${firstOrder.source}</td>
                    <td>${firstOrder.destination}</td>
                    <td>${firstOrder.assignedVehicle}</td>
                    <td>${firstOrder.assignedUnit}</td>
                    <td>${vehicleOrders.map(o => `(${o.index}, ${o.id})`).join(", ")}</td>
                </tr>
            `;
            deliveryTableBody.innerHTML += row;
        }
    }

    // Excel
    function exportToExcel() {
        let ordersSheet = [
            ["STT", "Grade", "ID", "Date", "Source", "Destination", "Assigned Vehicle ID", "Assigned Vehicle", "Assigned Unit"]
        ];
        data.orders.forEach((order, index) => {
            ordersSheet.push([
                index + 1,
                order.grade,
                order.id,
                order.date,
                order.source,
                order.destination,
                order.assignedVehicleID ?? "Không có chuyến phù hợp",
                order.assignedVehicle ?? "",
                order.assignedUnit ?? ""
            ]);
        });

        let deliveriesSheet = [
            ["Vehicle ID", "Source", "Destination", "Vehicle", "Unit", "Assigned Orders"]
        ];
        for (let vehicleID = 0; vehicleID <= maxVehicleID; vehicleID++) {
            let vehicleOrders = data.orders.filter(order => order.assignedVehicleID === vehicleID);
            if (vehicleOrders.length > 0) {
                let firstOrder = vehicleOrders[0];
                deliveriesSheet.push([
                    vehicleID,
                    firstOrder.source,
                    firstOrder.destination,
                    firstOrder.assignedVehicle,
                    firstOrder.assignedUnit,
                    vehicleOrders.map(o => `(${o.id})`).join(" | ")
                ]);
            }
        }

        let wb = XLSX.utils.book_new();
        let wsOrders = XLSX.utils.aoa_to_sheet(ordersSheet);
        let wsDeliveries = XLSX.utils.aoa_to_sheet(deliveriesSheet);

        XLSX.utils.book_append_sheet(wb, wsOrders, "Đơn hàng");
        XLSX.utils.book_append_sheet(wb, wsDeliveries, "Chuyến vận chuyển");

        XLSX.writeFile(wb, "result.xlsx");
    }

    exportToExcel();

    document.getElementById("showResult").addEventListener("click", function () {
        resultTable.style.display = "table";
        deliveryTable.style.display = "none";
    });

    document.getElementById("showDelivery").addEventListener("click", function () {
        resultTable.style.display = "none";
        deliveryTable.style.display = "table";
    });
});
