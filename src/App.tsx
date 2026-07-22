import React, { useState, useEffect } from 'react';
import { Navbar, NavTab } from './components/Navbar';
import { DashboardOverview } from './components/DashboardOverview';
import { VehicleList } from './components/VehicleList';
import { FuelTracker } from './components/FuelTracker';
import { MaintenanceHistory } from './components/MaintenanceHistory';
import { ServiceReminders } from './components/ServiceReminders';
import { VehicleDocuments } from './components/VehicleDocuments';
import { EmergencyContacts } from './components/EmergencyContacts';
import { NotificationsView } from './components/NotificationsView';
import { SettingsView } from './components/SettingsView';
import { LanguageProvider, useLanguage } from './context/LanguageContext';

import {
  Vehicle,
  FuelEntry,
  Maintenance,
  Reminder,
  Document,
  EmergencyContact,
  NotificationItem,
  AppSettings,
} from './types';
import { StorageService } from './utils/storage';

const AppContent: React.FC = () => {
  const { t } = useLanguage();
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');

  // State data
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [fuelEntries, setFuelEntries] = useState<FuelEntry[]>([]);
  const [maintenance, setMaintenance] = useState<Maintenance[]>([]);
  const [reminders, setReminders] = useState<Reminder[]>([]);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [emergencyContacts, setEmergencyContacts] = useState<EmergencyContact[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [settings, setSettings] = useState<AppSettings>(StorageService.getSettings());

  // Filter params passed when navigating from vehicle detail
  const [navVehicleFilter, setNavVehicleFilter] = useState('');
  const [navOpenAddForm, setNavOpenAddForm] = useState(false);

  // Load all data on mount
  const refreshAllData = () => {
    setVehicles(StorageService.getVehicles());
    setFuelEntries(StorageService.getFuelEntries());
    setMaintenance(StorageService.getMaintenance());
    setReminders(StorageService.getReminders());
    setDocuments(StorageService.getDocuments());
    setEmergencyContacts(StorageService.getEmergencyContacts());
    setNotifications(StorageService.getNotifications());
    setSettings(StorageService.getSettings());
  };

  useEffect(() => {
    refreshAllData();
  }, []);

  // Handlers for Vehicles
  const handleSaveVehicle = (v: Vehicle) => {
    StorageService.saveVehicle(v);
    setVehicles(StorageService.getVehicles());
  };

  const handleDeleteVehicle = (id: number) => {
    StorageService.deleteVehicle(id);
    setVehicles(StorageService.getVehicles());
  };

  // Handlers for Fuel
  const handleSaveFuelEntry = (f: FuelEntry) => {
    StorageService.saveFuelEntry(f);
    setFuelEntries(StorageService.getFuelEntries());
  };

  const handleDeleteFuelEntry = (id: number) => {
    StorageService.deleteFuelEntry(id);
    setFuelEntries(StorageService.getFuelEntries());
  };

  // Handlers for Maintenance
  const handleSaveMaintenance = (m: Maintenance) => {
    StorageService.saveMaintenance(m);
    setMaintenance(StorageService.getMaintenance());
  };

  const handleDeleteMaintenance = (id: number) => {
    StorageService.deleteMaintenance(id);
    setMaintenance(StorageService.getMaintenance());
  };

  // Handlers for Reminders
  const handleSaveReminder = (r: Reminder) => {
    StorageService.saveReminder(r);
    setReminders(StorageService.getReminders());
  };

  const handleDeleteReminder = (id: number) => {
    StorageService.deleteReminder(id);
    setReminders(StorageService.getReminders());
  };

  // Handlers for Documents
  const handleSaveDocument = (doc: Document) => {
    StorageService.saveDocument(doc);
    setDocuments(StorageService.getDocuments());
  };

  const handleDeleteDocument = (id: number) => {
    StorageService.deleteDocument(id);
    setDocuments(StorageService.getDocuments());
  };

  // Handlers for Emergency Contacts
  const handleSaveEmergencyContact = (c: EmergencyContact) => {
    StorageService.saveEmergencyContact(c);
    setEmergencyContacts(StorageService.getEmergencyContacts());
  };

  const handleDeleteEmergencyContact = (id: number) => {
    StorageService.deleteEmergencyContact(id);
    setEmergencyContacts(StorageService.getEmergencyContacts());
  };

  // Handlers for Notifications
  const handleSaveNotification = (n: NotificationItem) => {
    StorageService.addNotification(n);
    setNotifications(StorageService.getNotifications());
  };

  const handleDeleteNotification = (id: number) => {
    StorageService.deleteNotification(id);
    setNotifications(StorageService.getNotifications());
  };

  const handleMarkAllRead = () => {
    StorageService.markAllNotificationsRead();
    setNotifications(StorageService.getNotifications());
  };

  // Handlers for Settings & Reset
  const handleSaveSettings = (s: AppSettings) => {
    StorageService.saveSettings(s);
    setSettings(StorageService.getSettings());
  };

  const handleResetDatabase = () => {
    StorageService.resetToSeedData();
    refreshAllData();
  };

  // Cross-view navigation helper
  const handleNavigateWithFilter = (
    tab: NavTab,
    vehicleName?: string,
    openAdd: boolean = false
  ) => {
    setNavVehicleFilter(vehicleName || '');
    setNavOpenAddForm(openAdd);
    setActiveTab(tab);
  };

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900 font-sans flex flex-col antialiased selection:bg-blue-600 selection:text-white">
      {/* Navigation Header */}
      <Navbar
        activeTab={activeTab}
        setActiveTab={(tab) => {
          setNavVehicleFilter('');
          setNavOpenAddForm(false);
          setActiveTab(tab);
        }}
        pendingRemindersCount={
          reminders.filter(
            (r) =>
              r.status?.includes('Days Remaining') ||
              r.status === 'Due Today' ||
              r.status === 'Overdue'
          ).length
        }
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeTab === 'dashboard' && (
          <DashboardOverview
            vehicles={vehicles}
            fuelEntries={fuelEntries}
            maintenance={maintenance}
            reminders={reminders}
            documents={documents}
            setActiveTab={setActiveTab}
            onOpenAddVehicle={() => handleNavigateWithFilter('vehicles', '', true)}
            onOpenAddFuel={() => handleNavigateWithFilter('fuel', '', true)}
            onOpenAddMaintenance={() =>
              handleNavigateWithFilter('maintenance', '', true)
            }
            onOpenAddReminder={() =>
              handleNavigateWithFilter('reminders', '', true)
            }
            onOpenAddDocument={() =>
              handleNavigateWithFilter('documents', '', true)
            }
          />
        )}

        {activeTab === 'vehicles' && (
          <VehicleList
            vehicles={vehicles}
            onSaveVehicle={handleSaveVehicle}
            onDeleteVehicle={handleDeleteVehicle}
            onNavigateTab={(tab, vehicleName) =>
              handleNavigateWithFilter(tab, vehicleName, false)
            }
            initialAddOpen={navOpenAddForm}
          />
        )}

        {activeTab === 'fuel' && (
          <FuelTracker
            fuelEntries={fuelEntries}
            vehicles={vehicles}
            onSaveFuelEntry={handleSaveFuelEntry}
            onDeleteFuelEntry={handleDeleteFuelEntry}
            initialVehicleFilter={navVehicleFilter}
            initialAddOpen={navOpenAddForm}
          />
        )}

        {activeTab === 'maintenance' && (
          <MaintenanceHistory
            maintenance={maintenance}
            vehicles={vehicles}
            onSaveMaintenance={handleSaveMaintenance}
            onDeleteMaintenance={handleDeleteMaintenance}
            initialVehicleFilter={navVehicleFilter}
            initialAddOpen={navOpenAddForm}
          />
        )}

        {activeTab === 'reminders' && (
          <ServiceReminders
            reminders={reminders}
            vehicles={vehicles}
            onSaveReminder={handleSaveReminder}
            onDeleteReminder={handleDeleteReminder}
            initialVehicleFilter={navVehicleFilter}
            initialAddOpen={navOpenAddForm}
          />
        )}

        {activeTab === 'documents' && (
          <VehicleDocuments
            documents={documents}
            vehicles={vehicles}
            onSaveDocument={handleSaveDocument}
            onDeleteDocument={handleDeleteDocument}
            initialVehicleFilter={navVehicleFilter}
            initialAddOpen={navOpenAddForm}
          />
        )}

        {activeTab === 'emergency' && (
          <EmergencyContacts
            contacts={emergencyContacts}
            onSaveContact={handleSaveEmergencyContact}
            onDeleteContact={handleDeleteEmergencyContact}
            initialAddOpen={navOpenAddForm}
          />
        )}

        {activeTab === 'notifications' && (
          <NotificationsView
            notifications={notifications}
            onSaveNotification={handleSaveNotification}
            onDeleteNotification={handleDeleteNotification}
            onMarkAllRead={handleMarkAllRead}
          />
        )}

        {activeTab === 'settings' && (
          <SettingsView
            settings={settings}
            onSaveSettings={handleSaveSettings}
            onResetDatabase={handleResetDatabase}
            onImportBackup={refreshAllData}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 mt-12 py-6 text-slate-500 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-3">
          <p className="font-medium">
            © DriveCare — {t('appSubtitle')}.
          </p>
          <div className="flex items-center space-x-4 font-semibold text-slate-600">
            <span>Client-Side Local Storage</span>
            <span>•</span>
            <button
              onClick={() => setActiveTab('emergency')}
              className="hover:text-red-600 transition-colors"
            >
              {t('emergency')}
            </button>
            <span>•</span>
            <button
              onClick={() => setActiveTab('settings')}
              className="hover:text-blue-600 transition-colors"
            >
              {t('settings')}
            </button>
          </div>
        </div>
      </footer>
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <LanguageProvider>
      <AppContent />
    </LanguageProvider>
  );
};

export default App;
