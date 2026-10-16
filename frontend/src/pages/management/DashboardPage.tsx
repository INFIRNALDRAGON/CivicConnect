import React, { useState } from 'react';
import { useCategories, useReport } from '../../hooks/useRequests';
import { Report, ReportRequest } from '../../types';
import { dateInputValue, formatAge, formatDate, formatDuration } from '../../utils/dateFormat';
import { OPEN_STATUSES, STATUS_ORDER } from '../../utils/statusLabels';
import Alert from '../../components/common/Alert';
import Button from '../../components/common/Button';
import EmptyState from '../../components/common/EmptyState';
import Field from '../../components/common/Field';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import Panel from '../../components/common/Panel';
import StatusBadge from '../../components/common/StatusBadge';

type ViewKey = 'all' | 'open' | 'overdue' | 'resolved' | 'closed';

const VIEWS: Record<ViewKey, { label: string; matches: (request: ReportRequest) => boolean }> = {
  all: { label: 'All requests', matches: () => true },
  open: { label: 'Open', matches: (request) => OPEN_STATUSES.includes(request.status) },
  overdue: { label: 'Overdue', matches: (request) => request.overdue },
  resolved: { label: 'Resolved', matches: (request) => request.status === 'RESOLVED' },
  closed: { label: 'Closed', matches: (request) => request.status === 'CLOSED' }
};

const TH = 'px-3 py-2.5 border-b-2 border-ink text-left text-[0.9375rem] whitespace-nowrap';
const TH_NUM = `${TH} text-right`;
const TD = 'px-3 py-2.5 border-b border-line align-middle';
const TD_NUM = `${TD} text-right tabular-nums`;

const downloadSummary = (report: Report, from: string, to: string): void => {
  const header = ['Category', 'Requests', 'Open', 'Overdue', 'Resolved', 'Closed', 'Average hours to resolve'];
  const rows = report.byCategory.map((row) => [
    row.categoryName,
    row.total,
    row.open,
    row.overdue,
    row.resolved,
    row.closed,
    row.avgResolutionHours === null ? '' : row.avgResolutionHours.toFixed(1)
  ]);
  const safe = (cell: string | number): string => {
    const text = String(cell);
    const guarded = /^[=+\-@\t\r]/.test(text) ? `'${text}` : text;
    return `"${guarded.replace(/"/g, '""')}"`;
  };
  const text = [header, ...rows].map((line) => line.map(safe).join(',')).join('\r\n');
  const link = document.createElement('a');
  link.href = URL.createObjectURL(new Blob(['\uFEFF', text], { type: 'text/csv;charset=utf-8' }));
  link.download = `civicconnect-summary-${from}-to-${to}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(link.href), 1000);
};

const DashboardPage: React.FC = () => {
  const [from, setFrom] = useState(() => dateInputValue(6));
  const [to, setTo] = useState(() => dateInputValue(0));
  const [categoryId, setCategoryId] = useState('');
  const [view, setView] = useState<ViewKey>('all');

  const rangeInvalid = !from || !to || from > to;
  const { data: categories } = useCategories();
  const { data: report, loading, error, reload } = useReport({ from, to, categoryId }, !rangeInvalid);

  return (
    <div className='max-w-6xl'>
      <h1 className='text-3xl md:text-4xl'>Service activity</h1>
      <p className='mt-2 max-w-[62ch] text-muted'>Requests submitted in the selected period. Counts and references only, never requester details.</p>

      {/* Filters */}
      <form className='flex flex-wrap items-end gap-3.5 mt-6 p-4 bg-white border border-line rounded-xl' aria-label="Report filters" onSubmit={(event) => event.preventDefault()}>
        <Field id="report-from" label="From" error={from && to && from > to ? 'Start date must be before the end date.' : undefined}>
          <input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
        </Field>
        <Field id="report-to" label="To">
          <input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
        </Field>
        <Field id="report-category" label="Category">
          <select value={categoryId} onChange={(event) => setCategoryId(event.target.value)}>
            <option value="">All categories</option>
            {(categories || []).map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </Field>
      </form>

      {loading && !report && !rangeInvalid && <LoadingSpinner label="Loading the report" />}
      {error && (
        <Alert
          variant="error"
          title="The report did not load"
          action={
            <Button variant="secondary" size="sm" onClick={reload}>
              Try again
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}

      {report && report.total === 0 && <EmptyState title="No requests in this period">Try a longer date range or a different category.</EmptyState>}

      {report && report.total > 0 && (
        <>
          <p className='mt-7 mb-0 max-w-[40ch] font-display text-2xl font-semibold leading-snug' aria-live="polite">
            <strong className='font-extrabold'>{report.total}</strong> {report.total === 1 ? 'request' : 'requests'} between {formatDate(`${from}T12:00:00+02:00`)} and {formatDate(`${to}T12:00:00+02:00`)}.{' '}
            <strong className='font-extrabold'>{report.openCount}</strong> still open, <strong className='font-extrabold text-red-700'>{report.overdueCount}</strong> overdue.
          </p>

          {/* By status */}
          <Panel labelledBy="status-section">
            <h2 id="status-section" className='mb-3 text-xl'>
              Requests by status
            </h2>
            <div className='overflow-x-auto'>
              <table className='w-full border-collapse'>
                <thead>
                  <tr>
                    <th scope="col" className={TH}>
                      Status
                    </th>
                    <th scope="col" className={TH_NUM}>
                      Requests
                    </th>
                    <th scope="col" className={TH_NUM}>
                      Share
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {STATUS_ORDER.map((status) => (
                    <tr key={status}>
                      <td className={TD}>
                        <StatusBadge status={status} />
                      </td>
                      <td className={TD_NUM}>{report.totals[status] ?? 0}</td>
                      <td className={TD_NUM}>{Math.round(((report.totals[status] ?? 0) / report.total) * 100)}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>

          {/* By category */}
          <Panel labelledBy="category-section">
            <div className='flex flex-wrap items-center justify-between gap-3 mb-3'>
              <h2 id="category-section" className='text-xl'>
                By category and time to resolution
              </h2>
              <Button variant="secondary" size="sm" onClick={() => downloadSummary(report, from, to)}>
                Download summary (CSV)
              </Button>
            </div>
            <div className='overflow-x-auto'>
              <table className='w-full border-collapse'>
                <thead>
                  <tr>
                    <th scope="col" className={TH}>
                      Category
                    </th>
                    <th scope="col" className={TH_NUM}>
                      Requests
                    </th>
                    <th scope="col" className={TH_NUM}>
                      Open
                    </th>
                    <th scope="col" className={TH_NUM}>
                      Overdue
                    </th>
                    <th scope="col" className={TH_NUM}>
                      Resolved
                    </th>
                    <th scope="col" className={TH_NUM}>
                      Closed
                    </th>
                    <th scope="col" className={TH_NUM}>
                      Average time to resolve
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {report.byCategory.map((row) => (
                    <tr key={row.categoryId}>
                      <th scope="row" className={`${TD} text-left font-bold`}>
                        {row.categoryName}
                      </th>
                      <td className={TD_NUM}>{row.total}</td>
                      <td className={TD_NUM}>{row.open}</td>
                      <td className={`${TD_NUM} ${row.overdue > 0 ? 'font-bold text-red-700' : ''}`}>{row.overdue}</td>
                      <td className={TD_NUM}>{row.resolved}</td>
                      <td className={TD_NUM}>{row.closed}</td>
                      <td className={TD_NUM}>{formatDuration(row.avgResolutionHours)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>

          {/* Request list */}
          <Panel labelledBy="list-section">
            <div className='flex flex-wrap items-center justify-between gap-3 mb-3'>
              <h2 id="list-section" className='text-xl'>
                Requests in this period
              </h2>
              <div className='flex flex-wrap gap-1.5' role="group" aria-label="Show requests">
                {(Object.keys(VIEWS) as ViewKey[]).map((key) => (
                  <button
                    key={key}
                    type="button"
                    className={`min-h-10 px-3.5 border-2 rounded-full font-bold cursor-pointer ${view === key ? 'border-ink bg-ink text-white' : 'border-line-strong bg-white text-slate-900'}`}
                    aria-pressed={view === key}
                    onClick={() => setView(key)}
                  >
                    {VIEWS[key].label}
                  </button>
                ))}
              </div>
            </div>
            <div className='overflow-x-auto'>
              <table className='w-full border-collapse'>
                <thead>
                  <tr>
                    <th scope="col" className={TH}>
                      Reference
                    </th>
                    <th scope="col" className={TH}>
                      Category
                    </th>
                    <th scope="col" className={TH}>
                      Status
                    </th>
                    <th scope="col" className={TH}>
                      Age
                    </th>
                    <th scope="col" className={TH}>
                      Overdue
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {report.requests.filter(VIEWS[view].matches).map((request) => (
                    <tr key={request.id}>
                      <th scope="row" className={`${TD} text-left font-display font-extrabold tabular-nums`}>
                        {request.referenceNumber}
                      </th>
                      <td className={TD}>{request.categoryName}</td>
                      <td className={TD}>
                        <StatusBadge status={request.status} />
                      </td>
                      <td className={TD}>{formatAge(request.createdAt)}</td>
                      <td className={TD}>{request.overdue ? <span className='px-2.5 py-0.5 rounded-sm bg-red-700 text-white text-[0.9375rem] font-bold'>Overdue</span> : 'No'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
        </>
      )}
    </div>
  );
};

export default DashboardPage;
