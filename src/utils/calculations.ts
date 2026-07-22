// Mileage & Fuel Calculations (ported from MileageUtils & FuelReportUtils)
export const MileageUtils = {
  calculateDistance(previousOdometer: number, currentOdometer: number): number {
    return Math.max(0, currentOdometer - previousOdometer);
  },

  calculateMileage(distance: number, fuelQuantity: number): number {
    if (fuelQuantity <= 0) return 0;
    return distance / fuelQuantity;
  },

  calculateCostPerKm(amountPaid: number, distance: number): number {
    if (distance <= 0) return 0;
    return amountPaid / distance;
  },

  calculateMonthlyExpenses(amountList: number[]): number {
    return amountList.reduce((acc, curr) => acc + curr, 0);
  },

  calculateYearlyExpenses(amountList: number[]): number {
    return amountList.reduce((acc, curr) => acc + curr, 0);
  },
};

export const FuelReportUtils = {
  getTotalFuelCost(amountList: number[]): number {
    return amountList.reduce((acc, curr) => acc + curr, 0);
  },

  getTotalFuelQuantity(fuelList: number[]): number {
    return fuelList.reduce((acc, curr) => acc + curr, 0);
  },

  getTotalDistance(distanceList: number[]): number {
    return distanceList.reduce((acc, curr) => acc + curr, 0);
  },

  getAverageMileage(mileageList: number[]): number {
    if (mileageList.length === 0) return 0;
    const sum = mileageList.reduce((acc, curr) => acc + curr, 0);
    return sum / mileageList.length;
  },

  getHighestMileage(mileageList: number[]): number {
    if (mileageList.length === 0) return 0;
    return Math.max(...mileageList);
  },

  getLowestMileage(mileageList: number[]): number {
    if (mileageList.length === 0) return 0;
    return Math.min(...mileageList);
  },
};

// Reminder Status Utilities (ported from ReminderUtils.kt)
export const ReminderUtils = {
  calculateStatus(dueDateStr: string): string {
    if (!dueDateStr) return "Upcoming";

    try {
      // Handle both DD-MM-YYYY and YYYY-MM-DD
      let targetDate: Date;
      if (dueDateStr.includes("-")) {
        const parts = dueDateStr.split("-");
        if (parts[0].length === 4) {
          // YYYY-MM-DD
          targetDate = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
        } else if (parts[2].length === 4) {
          // DD-MM-YYYY
          targetDate = new Date(parseInt(parts[2]), parseInt(parts[1]) - 1, parseInt(parts[0]));
        } else {
          targetDate = new Date(dueDateStr);
        }
      } else {
        targetDate = new Date(dueDateStr);
      }

      const today = new Date();
      today.setHours(0, 0, 0, 0);
      targetDate.setHours(0, 0, 0, 0);

      const diffTime = targetDate.getTime() - today.getTime();
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

      if (diffDays > 0) {
        return `${diffDays} Days Remaining`;
      } else if (diffDays === 0) {
        return "Due Today";
      } else {
        return "Overdue";
      }
    } catch {
      return "Upcoming";
    }
  },
};
