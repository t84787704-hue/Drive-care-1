import {
  Vehicle,
  FuelEntry,
  Maintenance,
  Reminder,
  Document,
  EmergencyContact,
  NotificationItem,
  AppSettings,
} from '../types';

const KEYS = {
  VEHICLES: 'drivecare_vehicles',
  FUEL_ENTRIES: 'drivecare_fuel_entries',
  MAINTENANCE: 'drivecare_maintenance',
  REMINDERS: 'drivecare_reminders',
  DOCUMENTS: 'drivecare_documents',
  EMERGENCY: 'EmergencyData_contact',
  NOTIFICATIONS: 'NotificationData_text',
  SETTINGS: 'SettingsData_text',
};

// Initial Seed Data
const DEFAULT_VEHICLES: Vehicle[] = [
  {
    id: 1,
    vehicleName: 'Toyota Camry Hybrid',
    vehicleType: 'Sedan',
    brand: 'Toyota',
    model: 'Camry',
    manufacturingYear: '2022',
    registrationNumber: 'ABC-1234',
    engineNumber: '2AR-FXE-991',
    chassisNumber: 'JTEBU22F8820',
    fuelType: 'Hybrid',
    odometerReading: '28450',
    purchaseDate: '2022-03-15',
    insuranceDetails: 'State Farm #POL-889102',
    vehiclePhoto: '',
    country: 'USA',
    distanceUnit: 'km',
    notes: 'Primary family commuter vehicle.',
    createdAt: Date.now() - 1000000,
    updatedAt: Date.now() - 1000000,
  },
  {
    id: 2,
    vehicleName: 'Honda CR-V',
    vehicleType: 'SUV',
    brand: 'Honda',
    model: 'CR-V',
    manufacturingYear: '2021',
    registrationNumber: 'XYZ-5678',
    engineNumber: 'K24W2-441',
    chassisNumber: '7FARW2H59931',
    fuelType: 'Petrol',
    odometerReading: '41200',
    purchaseDate: '2021-08-10',
    insuranceDetails: 'Geico #POL-332901',
    vehiclePhoto: '',
    country: 'USA',
    distanceUnit: 'km',
    notes: 'Weekend road trip vehicle.',
    createdAt: Date.now() - 2000000,
    updatedAt: Date.now() - 2000000,
  },
];

const DEFAULT_FUEL: FuelEntry[] = [
  {
    id: 1,
    vehicleName: 'Toyota Camry Hybrid',
    fuelDate: '2026-07-20',
    fuelType: 'Hybrid/Petrol',
    fuelQuantity: '42.5',
    amountPaid: '63.75',
    currentOdometer: '28450',
    fuelStationName: 'Shell Express',
    notes: 'Full tank fill-up',
    createdAt: Date.now() - 800000,
  },
  {
    id: 2,
    vehicleName: 'Toyota Camry Hybrid',
    fuelDate: '2026-07-05',
    fuelType: 'Hybrid/Petrol',
    fuelQuantity: '40.0',
    amountPaid: '60.00',
    currentOdometer: '27920',
    fuelStationName: 'BP Highway',
    notes: 'Regular commute fuel',
    createdAt: Date.now() - 1500000,
  },
  {
    id: 3,
    vehicleName: 'Honda CR-V',
    fuelDate: '2026-07-18',
    fuelType: 'Petrol',
    fuelQuantity: '50.0',
    amountPaid: '80.00',
    currentOdometer: '41200',
    fuelStationName: 'Chevron',
    notes: 'Trip to national park',
    createdAt: Date.now() - 900000,
  },
];

const DEFAULT_MAINTENANCE: Maintenance[] = [
  {
    id: 1,
    vehicleName: 'Toyota Camry Hybrid',
    serviceTitle: 'Synthetic Oil & Filter Replacement',
    serviceType: 'Oil Change',
    serviceDate: '2026-06-10',
    currentOdometer: '27500',
    serviceCost: '89.99',
    workshopName: 'Toyota Certified Care Center',
    notes: 'Replaced synthetic engine oil, oil filter, and topped off coolant.',
    createdAt: Date.now() - 2500000,
    updatedAt: Date.now() - 2500000,
  },
  {
    id: 2,
    vehicleName: 'Honda CR-V',
    serviceTitle: 'Front Brake Pads & Tire Rotation',
    serviceType: 'Brake Service',
    serviceDate: '2026-05-02',
    currentOdometer: '39800',
    serviceCost: '245.50',
    workshopName: 'Precision Auto Garage',
    notes: 'Installed premium ceramic brake pads and balanced all 4 tires.',
    createdAt: Date.now() - 4000000,
    updatedAt: Date.now() - 4000000,
  },
];

const DEFAULT_REMINDERS: Reminder[] = [
  {
    id: 1,
    vehicleName: 'Toyota Camry Hybrid',
    reminderTitle: '30,000 KM Major Inspection',
    reminderType: 'Service Due',
    dueDate: '2026-08-25',
    currentOdometer: '28450',
    nextServiceOdometer: '30000',
    notes: 'Spark plug check, brake fluid replacement & cabin air filter.',
    status: '34 Days Remaining',
    createdAt: Date.now() - 500000,
    updatedAt: Date.now() - 500000,
  },
  {
    id: 2,
    vehicleName: 'Honda CR-V',
    reminderTitle: 'Annual Vehicle Insurance Renewal',
    reminderType: 'Insurance Renewal',
    dueDate: '2026-08-10',
    currentOdometer: '41200',
    nextServiceOdometer: '45000',
    notes: 'Renew policy with Geico before expiration.',
    status: '19 Days Remaining',
    createdAt: Date.now() - 600000,
    updatedAt: Date.now() - 600000,
  },
];

const DEFAULT_DOCUMENTS: Document[] = [
  {
    id: 1,
    vehicleName: 'Toyota Camry Hybrid',
    documentTitle: 'Comprehensive Auto Insurance Policy',
    documentType: 'Insurance Policy',
    documentNumber: 'POL-889102-SF',
    issueDate: '2026-03-15',
    expiryDate: '2027-03-15',
    notes: 'Includes full coverage, zero deductible glass, and roadside assistance.',
    createdAt: Date.now() - 3000000,
  },
  {
    id: 2,
    vehicleName: 'Honda CR-V',
    documentTitle: 'State Vehicle Registration Card',
    documentType: 'Registration / RC',
    documentNumber: 'RC-99201-GA',
    issueDate: '2025-08-10',
    expiryDate: '2026-08-10',
    notes: 'Keep physical copy stored in glove compartment.',
    createdAt: Date.now() - 5000000,
  },
];

const DEFAULT_EMERGENCY_CONTACTS: EmergencyContact[] = [
  {
    id: 1,
    contactName: 'AAA 24/7 Roadside Assistance',
    phoneNumber: '1-800-222-4357',
    contactCategory: 'Roadside Assistance',
    locationRegion: 'Nationwide / USA',
    notes: 'Free towing up to 100 miles, battery jumpstart, tire changes.',
    createdAt: Date.now(),
  },
  {
    id: 2,
    contactName: 'Mike - Precision Auto Garage',
    phoneNumber: '555-019-2831',
    contactCategory: 'Mechanic',
    locationRegion: 'Local Garage',
    notes: 'Primary trusted mechanic for non-warranty maintenance.',
    createdAt: Date.now(),
  },
  {
    id: 3,
    contactName: 'State Farm Emergency Claims Line',
    phoneNumber: '1-800-782-8332',
    contactCategory: 'Insurance Claims',
    locationRegion: 'National Hotline',
    notes: 'Policy #POL-889102 - 24/7 Incident hotline.',
    createdAt: Date.now(),
  },
];

const DEFAULT_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 1,
    title: 'Welcome to DriveCare',
    message: 'Your vehicle care suite is set up and ready to track fuel and service.',
    vehicleName: 'Toyota Camry Hybrid',
    timestamp: Date.now() - 200000,
    isRead: false,
  },
  {
    id: 2,
    title: 'Service Reminder Notice',
    message: 'Toyota Camry Hybrid 30,000 KM service due soon.',
    vehicleName: 'Toyota Camry Hybrid',
    timestamp: Date.now() - 100000,
    isRead: false,
  },
];

const DEFAULT_SETTINGS: AppSettings = {
  settingText: 'DriveCare operational',
  distanceUnit: 'km',
  currency: '$',
  notificationsEnabled: true,
  reminderDaysBefore: 7,
};

function getItem<T>(key: string, fallback: T): T {
  try {
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : fallback;
  } catch {
    return fallback;
  }
}

function setItem<T>(key: string, data: T): void {
  try {
    localStorage.setItem(key, JSON.stringify(data));
  } catch (err) {
    console.error('Failed to save to localStorage:', err);
  }
}

export const StorageService = {
  // Vehicles
  getVehicles(): Vehicle[] {
    return getItem(KEYS.VEHICLES, DEFAULT_VEHICLES);
  },
  saveVehicle(vehicle: Vehicle): Vehicle[] {
    const list = this.getVehicles();
    let updated: Vehicle[];
    if (vehicle.id && vehicle.id > 0) {
      updated = list.map((v) =>
        v.id === vehicle.id ? { ...vehicle, updatedAt: Date.now() } : v
      );
    } else {
      const newVehicle = {
        ...vehicle,
        id: Date.now(),
        createdAt: Date.now(),
        updatedAt: Date.now(),
      };
      updated = [newVehicle, ...list];
    }
    setItem(KEYS.VEHICLES, updated);
    return updated;
  },
  deleteVehicle(id: number): Vehicle[] {
    const list = this.getVehicles();
    const updated = list.filter((v) => v.id !== id);
    setItem(KEYS.VEHICLES, updated);
    return updated;
  },

  // Fuel
  getFuelEntries(): FuelEntry[] {
    return getItem(KEYS.FUEL_ENTRIES, DEFAULT_FUEL);
  },
  saveFuelEntry(entry: FuelEntry): FuelEntry[] {
    const list = this.getFuelEntries();
    const newEntry = {
      ...entry,
      id: entry.id && entry.id > 0 ? entry.id : Date.now(),
      createdAt: Date.now(),
    };
    const updated = [newEntry, ...list.filter((f) => f.id !== newEntry.id)];
    setItem(KEYS.FUEL_ENTRIES, updated);
    return updated;
  },
  deleteFuelEntry(id: number): FuelEntry[] {
    const list = this.getFuelEntries();
    const updated = list.filter((f) => f.id !== id);
    setItem(KEYS.FUEL_ENTRIES, updated);
    return updated;
  },

  // Maintenance
  getMaintenance(): Maintenance[] {
    return getItem(KEYS.MAINTENANCE, DEFAULT_MAINTENANCE);
  },
  saveMaintenance(m: Maintenance): Maintenance[] {
    const list = this.getMaintenance();
    const newM = {
      ...m,
      id: m.id && m.id > 0 ? m.id : Date.now(),
      createdAt: m.createdAt || Date.now(),
      updatedAt: Date.now(),
    };
    const updated = [newM, ...list.filter((item) => item.id !== newM.id)];
    setItem(KEYS.MAINTENANCE, updated);
    return updated;
  },
  deleteMaintenance(id: number): Maintenance[] {
    const list = this.getMaintenance();
    const updated = list.filter((m) => m.id !== id);
    setItem(KEYS.MAINTENANCE, updated);
    return updated;
  },

  // Reminders
  getReminders(): Reminder[] {
    return getItem(KEYS.REMINDERS, DEFAULT_REMINDERS);
  },
  saveReminder(r: Reminder): Reminder[] {
    const list = this.getReminders();
    const newR = {
      ...r,
      id: r.id && r.id > 0 ? r.id : Date.now(),
      createdAt: r.createdAt || Date.now(),
      updatedAt: Date.now(),
    };
    const updated = [newR, ...list.filter((item) => item.id !== newR.id)];
    setItem(KEYS.REMINDERS, updated);
    return updated;
  },
  deleteReminder(id: number): Reminder[] {
    const list = this.getReminders();
    const updated = list.filter((r) => r.id !== id);
    setItem(KEYS.REMINDERS, updated);
    return updated;
  },

  // Documents
  getDocuments(): Document[] {
    return getItem(KEYS.DOCUMENTS, DEFAULT_DOCUMENTS);
  },
  saveDocument(doc: Document): Document[] {
    const list = this.getDocuments();
    const newDoc = {
      ...doc,
      id: doc.id && doc.id > 0 ? doc.id : Date.now(),
      createdAt: doc.createdAt || Date.now(),
    };
    const updated = [newDoc, ...list.filter((d) => d.id !== newDoc.id)];
    setItem(KEYS.DOCUMENTS, updated);
    return updated;
  },
  deleteDocument(id: number): Document[] {
    const list = this.getDocuments();
    const updated = list.filter((d) => d.id !== id);
    setItem(KEYS.DOCUMENTS, updated);
    return updated;
  },

  // Emergency Contacts
  getEmergencyContacts(): EmergencyContact[] {
    return getItem(KEYS.EMERGENCY, DEFAULT_EMERGENCY_CONTACTS);
  },
  saveEmergencyContact(contact: EmergencyContact): EmergencyContact[] {
    const list = this.getEmergencyContacts();
    const newContact = {
      ...contact,
      id: contact.id && contact.id > 0 ? contact.id : Date.now(),
      createdAt: Date.now(),
    };
    const updated = [newContact, ...list.filter((c) => c.id !== newContact.id)];
    setItem(KEYS.EMERGENCY, updated);
    return updated;
  },
  deleteEmergencyContact(id: number): EmergencyContact[] {
    const list = this.getEmergencyContacts();
    const updated = list.filter((c) => c.id !== id);
    setItem(KEYS.EMERGENCY, updated);
    return updated;
  },

  // Notifications
  getNotifications(): NotificationItem[] {
    return getItem(KEYS.NOTIFICATIONS, DEFAULT_NOTIFICATIONS);
  },
  addNotification(n: NotificationItem): NotificationItem[] {
    const list = this.getNotifications();
    const newNotif = {
      ...n,
      id: n.id && n.id > 0 ? n.id : Date.now(),
      timestamp: n.timestamp || Date.now(),
    };
    const updated = [newNotif, ...list];
    setItem(KEYS.NOTIFICATIONS, updated);
    return updated;
  },
  deleteNotification(id: number): NotificationItem[] {
    const list = this.getNotifications();
    const updated = list.filter((n) => n.id !== id);
    setItem(KEYS.NOTIFICATIONS, updated);
    return updated;
  },
  markAllNotificationsRead(): NotificationItem[] {
    const list = this.getNotifications();
    const updated = list.map((n) => ({ ...n, isRead: true }));
    setItem(KEYS.NOTIFICATIONS, updated);
    return updated;
  },

  // Settings
  getSettings(): AppSettings {
    return getItem(KEYS.SETTINGS, DEFAULT_SETTINGS);
  },
  saveSettings(settings: AppSettings): AppSettings {
    setItem(KEYS.SETTINGS, settings);
    return settings;
  },

  // Import & Restore Backup Data
  importBackupData(data: any): void {
    if (Array.isArray(data.vehicles)) setItem(KEYS.VEHICLES, data.vehicles);
    if (Array.isArray(data.fuelEntries)) setItem(KEYS.FUEL_ENTRIES, data.fuelEntries);
    if (Array.isArray(data.maintenance)) setItem(KEYS.MAINTENANCE, data.maintenance);
    if (Array.isArray(data.reminders)) setItem(KEYS.REMINDERS, data.reminders);
    if (Array.isArray(data.documents)) setItem(KEYS.DOCUMENTS, data.documents);
    if (Array.isArray(data.emergencyContacts)) setItem(KEYS.EMERGENCY, data.emergencyContacts);
    if (data.settings && typeof data.settings === 'object') setItem(KEYS.SETTINGS, data.settings);
  },

  // Reset to seed data
  resetToSeedData(): void {
    setItem(KEYS.VEHICLES, DEFAULT_VEHICLES);
    setItem(KEYS.FUEL_ENTRIES, DEFAULT_FUEL);
    setItem(KEYS.MAINTENANCE, DEFAULT_MAINTENANCE);
    setItem(KEYS.REMINDERS, DEFAULT_REMINDERS);
    setItem(KEYS.DOCUMENTS, DEFAULT_DOCUMENTS);
    setItem(KEYS.EMERGENCY, DEFAULT_EMERGENCY_CONTACTS);
    setItem(KEYS.NOTIFICATIONS, DEFAULT_NOTIFICATIONS);
    setItem(KEYS.SETTINGS, DEFAULT_SETTINGS);
  },
};
