import React, { useState, useRef } from 'react';
import {
  Settings,
  Save,
  CheckCircle2,
  Trash2,
  Database,
  Bell,
  Globe,
  Download,
  FileJson,
  Upload,
  AlertCircle,
} from 'lucide-react';
import { AppSettings } from '../types';
import { useLanguage } from '../context/LanguageContext';
import { LANGUAGES, Language } from '../utils/i18n';
import { StorageService } from '../utils/storage';

interface SettingsViewProps {
  settings: AppSettings;
  onSaveSettings: (settings: AppSettings) => void;
  onResetDatabase: () => void;
  onImportBackup?: () => void;
}

export const SettingsView: React.FC<SettingsViewProps> = ({
  settings,
  onSaveSettings,
  onResetDatabase,
  onImportBackup,
}) => {
  const { t, language, setLanguage } = useLanguage();
  const [formData, setFormData] = useState<AppSettings>(settings);
  const [savedSuccess, setSavedSuccess] = useState(false);
  const [importSuccessMsg, setImportSuccessMsg] = useState('');
  const [importErrorMsg, setImportErrorMsg] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSaveSettings(formData);
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 3000);
  };

  const handleExportJson = () => {
    const backupData = {
      app: 'DriveCare',
      version: '1.0',
      exportDate: new Date().toISOString(),
      vehicles: StorageService.getVehicles(),
      fuelEntries: StorageService.getFuelEntries(),
      maintenance: StorageService.getMaintenance(),
      reminders: StorageService.getReminders(),
      documents: StorageService.getDocuments(),
      emergencyContacts: StorageService.getEmergencyContacts(),
      settings: StorageService.getSettings(),
    };

    const dataStr =
      'data:text/json;charset=utf-8,' +
      encodeURIComponent(JSON.stringify(backupData, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    const dateStamp = new Date().toISOString().slice(0, 10);
    downloadAnchor.setAttribute(
      'download',
      `drivecare-backup-${dateStamp}.json`
    );
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const content = event.target?.result as string;
        const parsedData = JSON.parse(content);

        if (!parsedData || typeof parsedData !== 'object') {
          throw new Error('Invalid structure');
        }

        // Perform import via StorageService
        StorageService.importBackupData(parsedData);
        if (onImportBackup) {
          onImportBackup();
        }

        setImportSuccessMsg(t('importSuccess'));
        setImportErrorMsg('');
        setTimeout(() => setImportSuccessMsg(''), 4000);
      } catch (err) {
        setImportErrorMsg(t('importError'));
        setImportSuccessMsg('');
        setTimeout(() => setImportErrorMsg(''), 4000);
      }

      // Reset file input value
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    };

    reader.readAsText(file);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
            <Settings className="w-6 h-6 text-slate-700" />
            <span>{t('settingsTitle')}</span>
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {t('settingsSubtitle')}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleExportJson}
            className="flex items-center gap-1.5 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-extrabold shadow transition-all"
          >
            <Download className="w-4 h-4" />
            <span>{t('exportJsonBtn')}</span>
          </button>
        </div>
      </div>

      {savedSuccess && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl text-xs font-bold flex items-center gap-2 animate-in fade-in duration-150">
          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          <span>{t('settingsSaved')}</span>
        </div>
      )}

      {importSuccessMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl text-xs font-bold flex items-center gap-2 animate-in fade-in duration-150">
          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          <span>{importSuccessMsg}</span>
        </div>
      )}

      {importErrorMsg && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-800 rounded-2xl text-xs font-bold flex items-center gap-2 animate-in fade-in duration-150">
          <AlertCircle className="w-4 h-4 text-red-600" />
          <span>{importErrorMsg}</span>
        </div>
      )}

      {/* Storage & Backup Card at top for high visibility */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm space-y-4">
        <h2 className="text-sm font-extrabold text-slate-900 uppercase tracking-wider flex items-center gap-2">
          <Database className="w-4 h-4 text-blue-600" />
          <span>{t('dataStorage')}</span>
        </h2>
        <p className="text-xs text-slate-500">
          {t('storageDesc')}
        </p>

        {/* Export Data JSON */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between bg-blue-50/60 p-4 rounded-xl border border-blue-200 gap-3">
          <div className="flex items-start gap-3">
            <div className="p-2 bg-blue-100 rounded-lg text-blue-700 mt-0.5 shrink-0">
              <FileJson className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-extrabold text-blue-950 block">
                {t('dataExportTitle')}
              </span>
              <span className="text-[11px] text-blue-800/80 block mt-0.5 max-w-md">
                {t('dataExportDesc')}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={handleExportJson}
            className="flex items-center justify-center gap-1.5 px-4 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold shadow transition-all shrink-0"
          >
            <Download className="w-4 h-4" />
            <span>{t('exportJsonBtn')}</span>
          </button>
        </div>

        {/* Import & Restore Data JSON */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between bg-emerald-50/60 p-4 rounded-xl border border-emerald-200 gap-3">
          <div className="flex items-start gap-3">
            <div className="p-2 bg-emerald-100 rounded-lg text-emerald-700 mt-0.5 shrink-0">
              <Upload className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-extrabold text-emerald-950 block">
                {t('dataImportTitle')}
              </span>
              <span className="text-[11px] text-emerald-800/80 block mt-0.5 max-w-md">
                {t('dataImportDesc')}
              </span>
            </div>
          </div>

          <input
            type="file"
            ref={fileInputRef}
            accept=".json,application/json"
            onChange={handleFileUpload}
            className="hidden"
          />

          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="flex items-center justify-center gap-1.5 px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow transition-all shrink-0"
          >
            <Upload className="w-4 h-4" />
            <span>{t('importJsonBtn')}</span>
          </button>
        </div>

        {/* Reset Data */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between bg-red-50/50 p-4 rounded-xl border border-red-200 gap-3">
          <div>
            <span className="text-xs font-extrabold text-red-900 block">
              {t('resetData')}
            </span>
            <span className="text-[11px] text-red-700 block">
              {t('resetDesc')}
            </span>
          </div>

          <button
            type="button"
            onClick={() => {
              if (window.confirm(t('confirmReset'))) {
                onResetDatabase();
              }
            }}
            className="flex items-center justify-center gap-1.5 px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-xl text-xs font-bold shadow transition-all shrink-0"
          >
            <Trash2 className="w-4 h-4" />
            <span>{t('resetBtn')}</span>
          </button>
        </div>
      </div>

      {/* Settings Form */}
      <form onSubmit={handleSubmit} className="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm space-y-6">
        {/* Regional & Language Settings */}
        <div>
          <h2 className="text-sm font-extrabold text-slate-900 uppercase tracking-wider mb-3 flex items-center gap-2">
            <Globe className="w-4 h-4 text-blue-600" />
            <span>{t('regionalSettings')}</span>
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs font-semibold text-slate-700">
            {/* Language Selector */}
            <div>
              <label className="block mb-1">{t('appLanguage')}</label>
              <select
                value={language}
                onChange={(e) => setLanguage(e.target.value as Language)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              >
                {LANGUAGES.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {lang.flag} {lang.nativeName} ({lang.name})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block mb-1">{t('distanceUnit')}</label>
              <select
                value={formData.distanceUnit}
                onChange={(e) =>
                  setFormData({ ...formData, distanceUnit: e.target.value })
                }
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              >
                <option value="km">Kilometers (km)</option>
                <option value="miles">Miles (mi)</option>
              </select>
            </div>

            <div>
              <label className="block mb-1">{t('currencySymbol')}</label>
              <select
                value={formData.currency}
                onChange={(e) =>
                  setFormData({ ...formData, currency: e.target.value })
                }
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              >
                <option value="$">US Dollar ($)</option>
                <option value="€">Euro (€)</option>
                <option value="£">British Pound (£)</option>
                <option value="₹">Indian Rupee (₹)</option>
                <option value="A$">Australian Dollar (A$)</option>
                <option value="C$">Canadian Dollar (C$)</option>
              </select>
            </div>
          </div>
        </div>

        {/* Notifications & Reminders */}
        <div className="pt-4 border-t border-slate-100">
          <h2 className="text-sm font-extrabold text-slate-900 uppercase tracking-wider mb-3 flex items-center gap-2">
            <Bell className="w-4 h-4 text-amber-600" />
            <span>{t('remindersAndNotifications')}</span>
          </h2>

          <div className="space-y-3">
            <label className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-200/80 cursor-pointer">
              <input
                type="checkbox"
                checked={formData.notificationsEnabled}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    notificationsEnabled: e.target.checked,
                  })
                }
                className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
              />
              <div>
                <span className="text-xs font-bold text-slate-800 block">
                  {t('enableReminders')}
                </span>
                <span className="text-[11px] text-slate-500 block">
                  {t('remindersDesc')}
                </span>
              </div>
            </label>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                {t('advanceNotice')}
              </label>
              <select
                value={formData.reminderDaysBefore}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    reminderDaysBefore: parseInt(e.target.value) || 7,
                  })
                }
                className="w-full sm:w-64 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
              >
                <option value={3}>3 Days Before Due Date</option>
                <option value={7}>7 Days Before Due Date (Default)</option>
                <option value={14}>14 Days Before Due Date</option>
                <option value={30}>30 Days Before Due Date</option>
              </select>
            </div>
          </div>
        </div>

        {/* Form Action */}
        <div className="pt-4 border-t border-slate-100 flex items-center justify-end">
          <button
            type="submit"
            className="flex items-center gap-1.5 px-6 py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-extrabold shadow transition-all"
          >
            <Save className="w-4 h-4" />
            <span>{t('savePreferences')}</span>
          </button>
        </div>
      </form>
    </div>
  );
};
