import React from 'react';
import {
  Car,
  Fuel,
  Wrench,
  BellRing,
  FileText,
  PlusCircle,
  ChevronRight,
  ShieldAlert,
} from 'lucide-react';
import {
  Vehicle,
  FuelEntry,
  Maintenance,
  Reminder,
  Document,
} from '../types';
import { FuelReportUtils, ReminderUtils } from '../utils/calculations';
import { NavTab } from './Navbar';
import { useLanguage } from '../context/LanguageContext';

interface DashboardOverviewProps {
  vehicles: Vehicle[];
  fuelEntries: FuelEntry[];
  maintenance: Maintenance[];
  reminders: Reminder[];
  documents: Document[];
  setActiveTab: (tab: NavTab) => void;
  onOpenAddVehicle: () => void;
  onOpenAddFuel: () => void;
  onOpenAddMaintenance: () => void;
  onOpenAddReminder: () => void;
  onOpenAddDocument: () => void;
}

export const DashboardOverview: React.FC<DashboardOverviewProps> = ({
  vehicles,
  fuelEntries,
  maintenance,
  reminders,
  documents,
  setActiveTab,
  onOpenAddVehicle,
  onOpenAddFuel,
  onOpenAddMaintenance,
  onOpenAddReminder,
  onOpenAddDocument,
}) => {
  const { t } = useLanguage();

  // Calculations
  const totalVehicles = vehicles.length;

  const totalFuelCost = FuelReportUtils.getTotalFuelCost(
    fuelEntries.map((f) => parseFloat(f.amountPaid) || 0)
  );

  const totalMaintenanceCost = maintenance.reduce(
    (sum, m) => sum + (parseFloat(m.serviceCost) || 0),
    0
  );

  const upcomingReminders = reminders.filter((r) => {
    const status = ReminderUtils.calculateStatus(r.dueDate);
    return status.includes('Days Remaining') || status === 'Due Today' || status === 'Overdue';
  });

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white rounded-2xl p-6 shadow-md border border-slate-800 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            {t('overviewTitle')} 👋
          </h1>
          <p className="text-slate-300 text-sm mt-1 max-w-xl">
            {t('overviewSubtitle')}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={onOpenAddVehicle}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-sm font-semibold shadow transition-all"
          >
            <PlusCircle className="w-4 h-4" />
            <span>{t('addVehicle')}</span>
          </button>
          <button
            onClick={onOpenAddFuel}
            className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-sm font-semibold shadow transition-all"
          >
            <Fuel className="w-4 h-4" />
            <span>{t('addFuelLog')}</span>
          </button>
        </div>
      </div>

      {/* Metrics Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Metric 1: Vehicles */}
        <div
          onClick={() => setActiveTab('vehicles')}
          className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              {t('myVehicles')}
            </span>
            <div className="p-2.5 rounded-xl bg-blue-50 text-blue-600 group-hover:scale-110 transition-transform">
              <Car className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline justify-between">
            <span className="text-3xl font-extrabold text-slate-900">
              {totalVehicles}
            </span>
            <span className="text-xs text-blue-600 font-semibold flex items-center group-hover:translate-x-1 rtl:group-hover:-translate-x-1 transition-transform">
              {t('viewAll')} <ChevronRight className="w-3.5 h-3.5 rtl:rotate-180" />
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-2">
            {vehicles.map((v) => v.vehicleName).join(', ') || t('noVehiclesYet')}
          </p>
        </div>

        {/* Metric 2: Service Reminders */}
        <div
          onClick={() => setActiveTab('reminders')}
          className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              {t('activeReminders')}
            </span>
            <div className="p-2.5 rounded-xl bg-amber-50 text-amber-600 group-hover:scale-110 transition-transform">
              <BellRing className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline justify-between">
            <span className="text-3xl font-extrabold text-slate-900">
              {upcomingReminders.length}
            </span>
            <span className="text-xs text-amber-600 font-semibold flex items-center group-hover:translate-x-1 rtl:group-hover:-translate-x-1 transition-transform">
              {t('viewAll')} <ChevronRight className="w-3.5 h-3.5 rtl:rotate-180" />
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-2">
            {upcomingReminders.length > 0
              ? `${upcomingReminders[0]?.reminderTitle} (${upcomingReminders[0]?.vehicleName})`
              : t('noRemindersYet')}
          </p>
        </div>

        {/* Metric 3: Total Fuel Spent */}
        <div
          onClick={() => setActiveTab('fuel')}
          className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              {t('fuelExpenses')}
            </span>
            <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600 group-hover:scale-110 transition-transform">
              <Fuel className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline justify-between">
            <span className="text-3xl font-extrabold text-slate-900">
              ${totalFuelCost.toFixed(2)}
            </span>
            <span className="text-xs text-emerald-600 font-semibold flex items-center group-hover:translate-x-1 rtl:group-hover:-translate-x-1 transition-transform">
              {t('viewAll')} <ChevronRight className="w-3.5 h-3.5 rtl:rotate-180" />
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-2">
            {fuelEntries.length > 0 ? `${fuelEntries.length} entries` : t('noFuelYet')}
          </p>
        </div>

        {/* Metric 4: Maintenance Expense */}
        <div
          onClick={() => setActiveTab('maintenance')}
          className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-all cursor-pointer group"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              {t('maintenanceCosts')}
            </span>
            <div className="p-2.5 rounded-xl bg-purple-50 text-purple-600 group-hover:scale-110 transition-transform">
              <Wrench className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline justify-between">
            <span className="text-3xl font-extrabold text-slate-900">
              ${totalMaintenanceCost.toFixed(2)}
            </span>
            <span className="text-xs text-purple-600 font-semibold flex items-center group-hover:translate-x-1 rtl:group-hover:-translate-x-1 transition-transform">
              {t('viewAll')} <ChevronRight className="w-3.5 h-3.5 rtl:rotate-180" />
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-2">
            {maintenance.length > 0 ? `${maintenance.length} records` : t('noMaintenanceYet')}
          </p>
        </div>
      </div>

      {/* Quick Action Navigation Grid */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm">
        <h2 className="text-lg font-bold text-slate-900 mb-4 flex items-center gap-2">
          <span>{t('quickActions')}</span>
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
          <button
            onClick={() => setActiveTab('vehicles')}
            className="flex flex-col items-center justify-center p-4 rounded-xl bg-slate-50 hover:bg-blue-50/80 text-slate-700 hover:text-blue-700 border border-slate-200/60 hover:border-blue-200 transition-all text-center group"
          >
            <Car className="w-7 h-7 text-blue-600 mb-2 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-bold">{t('myVehicles')}</span>
          </button>

          <button
            onClick={() => setActiveTab('reminders')}
            className="flex flex-col items-center justify-center p-4 rounded-xl bg-slate-50 hover:bg-amber-50/80 text-slate-700 hover:text-amber-700 border border-slate-200/60 hover:border-amber-200 transition-all text-center group"
          >
            <BellRing className="w-7 h-7 text-amber-600 mb-2 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-bold">{t('reminders')}</span>
          </button>

          <button
            onClick={() => setActiveTab('fuel')}
            className="flex flex-col items-center justify-center p-4 rounded-xl bg-slate-50 hover:bg-emerald-50/80 text-slate-700 hover:text-emerald-700 border border-slate-200/60 hover:border-emerald-200 transition-all text-center group"
          >
            <Fuel className="w-7 h-7 text-emerald-600 mb-2 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-bold">{t('fuelTracker')}</span>
          </button>

          <button
            onClick={() => setActiveTab('maintenance')}
            className="flex flex-col items-center justify-center p-4 rounded-xl bg-slate-50 hover:bg-purple-50/80 text-slate-700 hover:text-purple-700 border border-slate-200/60 hover:border-purple-200 transition-all text-center group"
          >
            <Wrench className="w-7 h-7 text-purple-600 mb-2 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-bold">{t('maintenance')}</span>
          </button>

          <button
            onClick={() => setActiveTab('documents')}
            className="flex flex-col items-center justify-center p-4 rounded-xl bg-slate-50 hover:bg-indigo-50/80 text-slate-700 hover:text-indigo-700 border border-slate-200/60 hover:border-indigo-200 transition-all text-center group"
          >
            <FileText className="w-7 h-7 text-indigo-600 mb-2 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-bold">{t('documents')}</span>
          </button>

          <button
            onClick={() => setActiveTab('emergency')}
            className="flex flex-col items-center justify-center p-4 rounded-xl bg-slate-50 hover:bg-red-50/80 text-slate-700 hover:text-red-700 border border-slate-200/60 hover:border-red-200 transition-all text-center group"
          >
            <ShieldAlert className="w-7 h-7 text-red-600 mb-2 group-hover:scale-110 transition-transform" />
            <span className="text-xs font-bold">{t('emergency')}</span>
          </button>
        </div>
      </div>

      {/* Main Two Columns: Vehicles Overview & Reminders Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Vehicles List Box */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <Car className="w-5 h-5 text-blue-600" />
                <span>{t('myVehicles')} ({vehicles.length})</span>
              </h2>
              <button
                onClick={onOpenAddVehicle}
                className="text-xs font-semibold text-blue-600 hover:text-blue-700 flex items-center gap-1"
              >
                <PlusCircle className="w-3.5 h-3.5" />
                <span>{t('addVehicle')}</span>
              </button>
            </div>

            {vehicles.length === 0 ? (
              <div className="p-8 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">
                <Car className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <p className="text-sm font-medium text-slate-600">{t('noVehiclesYet')}</p>
                <button
                  onClick={onOpenAddVehicle}
                  className="mt-3 px-4 py-1.5 bg-blue-600 text-white rounded-lg text-xs font-semibold"
                >
                  {t('addVehicle')}
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {vehicles.map((v) => (
                  <div
                    key={v.id}
                    onClick={() => setActiveTab('vehicles')}
                    className="p-4 rounded-xl bg-slate-50 hover:bg-slate-100/80 border border-slate-200/70 transition-all cursor-pointer flex items-center justify-between"
                  >
                    <div>
                      <h3 className="font-bold text-slate-900 text-sm">{v.vehicleName}</h3>
                      <div className="flex flex-wrap gap-2 text-xs text-slate-500 mt-1">
                        <span className="bg-slate-200/60 px-2 py-0.5 rounded text-slate-700 font-medium">
                          {v.brand} {v.model}
                        </span>
                        <span className="bg-blue-50 text-blue-700 px-2 py-0.5 rounded font-medium">
                          {v.vehicleType}
                        </span>
                        <span className="bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded font-medium">
                          {v.fuelType}
                        </span>
                      </div>
                    </div>
                    <div className="text-right rtl:text-left">
                      <span className="text-xs font-bold text-slate-800 block">
                        {v.odometerReading} {v.distanceUnit || 'km'}
                      </span>
                      <span className="text-[11px] text-slate-400 block mt-0.5">
                        {v.registrationNumber || 'N/A'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="mt-4 pt-4 border-t border-slate-100 text-right rtl:text-left">
            <button
              onClick={() => setActiveTab('vehicles')}
              className="text-xs font-bold text-blue-600 hover:underline inline-flex items-center gap-1"
            >
              <span>{t('viewAll')}</span>
              <ChevronRight className="w-3.5 h-3.5 rtl:rotate-180" />
            </button>
          </div>
        </div>

        {/* Reminders Feed */}
        <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <BellRing className="w-5 h-5 text-amber-600" />
                <span>{t('reminders')} ({reminders.length})</span>
              </h2>
              <button
                onClick={onOpenAddReminder}
                className="text-xs font-semibold text-amber-600 hover:text-amber-700 flex items-center gap-1"
              >
                <PlusCircle className="w-3.5 h-3.5" />
                <span>{t('addReminder')}</span>
              </button>
            </div>

            {reminders.length === 0 ? (
              <div className="p-8 text-center bg-slate-50 rounded-xl border border-dashed border-slate-200">
                <BellRing className="w-10 h-10 text-slate-300 mx-auto mb-2" />
                <p className="text-sm font-medium text-slate-600">{t('noRemindersYet')}</p>
                <button
                  onClick={onOpenAddReminder}
                  className="mt-3 px-4 py-1.5 bg-amber-600 text-white rounded-lg text-xs font-semibold"
                >
                  {t('addReminder')}
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {reminders.slice(0, 4).map((r) => {
                  const statusStr = ReminderUtils.calculateStatus(r.dueDate);
                  const isDueToday = statusStr === 'Due Today';
                  const isOverdue = statusStr === 'Overdue';

                  return (
                    <div
                      key={r.id}
                      onClick={() => setActiveTab('reminders')}
                      className="p-3.5 rounded-xl bg-slate-50 hover:bg-slate-100/80 border border-slate-200/70 transition-all cursor-pointer flex items-center justify-between"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-extrabold text-slate-900">
                            {r.reminderTitle}
                          </span>
                          <span className="text-[11px] px-2 py-0.5 rounded-md bg-slate-200/70 text-slate-700 font-medium">
                            {r.vehicleName}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-1">
                          {r.reminderType} • {r.dueDate}
                        </p>
                      </div>
                      <span
                        className={`text-xs font-bold px-2.5 py-1 rounded-lg ${
                          isOverdue
                            ? 'bg-red-100 text-red-700'
                            : isDueToday
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-emerald-100 text-emerald-800'
                        }`}
                      >
                        {statusStr}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          <div className="mt-4 pt-4 border-t border-slate-100 text-right rtl:text-left">
            <button
              onClick={() => setActiveTab('reminders')}
              className="text-xs font-bold text-amber-600 hover:underline inline-flex items-center gap-1"
            >
              <span>{t('viewAll')}</span>
              <ChevronRight className="w-3.5 h-3.5 rtl:rotate-180" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
