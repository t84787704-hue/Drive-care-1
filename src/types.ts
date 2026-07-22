export interface Vehicle {
  id: number;
  vehicleName: string;
  vehicleType: string;
  brand: string;
  model: string;
  manufacturingYear: string;
  registrationNumber: string;
  engineNumber: string;
  chassisNumber: string;
  fuelType: string;
  odometerReading: string;
  purchaseDate: string;
  insuranceDetails: string;
  vehiclePhoto: string;
  country: string;
  distanceUnit: string;
  notes: string;
  createdAt: number;
  updatedAt: number;
}

export interface FuelEntry {
  id: number;
  vehicleName: string;
  fuelDate: string;
  fuelType: string;
  fuelQuantity: string;
  amountPaid: string;
  currentOdometer: string;
  fuelStationName: string;
  notes: string;
  createdAt: number;
}

export interface Maintenance {
  id: number;
  vehicleName: string;
  serviceTitle: string;
  serviceType: string;
  serviceDate: string;
  currentOdometer: string;
  serviceCost: string;
  workshopName: string;
  notes: string;
  createdAt: number;
  updatedAt: number;
}

export interface Reminder {
  id: number;
  vehicleName: string;
  reminderTitle: string;
  reminderType: string;
  dueDate: string;
  currentOdometer: string;
  nextServiceOdometer: string;
  notes: string;
  status: string;
  createdAt: number;
  updatedAt: number;
}

export interface Document {
  id: number;
  vehicleName: string;
  documentTitle: string;
  documentType: string;
  documentNumber: string;
  issueDate: string;
  expiryDate: string;
  notes: string;
  createdAt: number;
}

export interface EmergencyContact {
  id: number;
  contactName: string;
  phoneNumber: string;
  contactCategory: string;
  locationRegion: string;
  notes: string;
  createdAt: number;
}

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  vehicleName?: string;
  timestamp: number;
  isRead: boolean;
}

export interface AppSettings {
  settingText: string;
  distanceUnit: string;
  currency: string;
  notificationsEnabled: boolean;
  reminderDaysBefore: number;
}
