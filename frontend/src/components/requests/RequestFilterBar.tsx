import React, { useEffect, useState } from 'react';
import { Category, QueueFilters } from '../../types';
import Button from '../common/Button';
import { CONTROL_CLASS } from '../common/Field';
import { STATUS_ORDER, statusLabel } from '../../utils/statusLabels';

interface RequestFilterBarProps {
  values: QueueFilters;
  categories: Category[] | null;
  onChange: (patch: Partial<QueueFilters>) => void;
  onReset: () => void;
}

const LABEL = 'font-bold';

const RequestFilterBar: React.FC<RequestFilterBarProps> = ({ values, categories, onChange, onReset }) => {
  const [search, setSearch] = useState(values.search);

  useEffect(() => {
    if (search === values.search) return undefined;
    const timer = setTimeout(() => onChange({ search }), 300);
    return () => clearTimeout(timer);
  }, [search]);

  const active = Boolean(values.search || values.categoryId || values.status || values.sort !== 'age_desc');

  return (
    <form className='flex flex-wrap items-end gap-3.5 mt-6 p-4 bg-white border border-line rounded-xl' role="search" aria-label="Filter requests" onSubmit={(event) => event.preventDefault()}>
      <div className='flex flex-col gap-1.5 flex-[1_1_240px]'>
        <label htmlFor="filter-search" className={LABEL}>
          Search
        </label>
        <input id="filter-search" type="search" className={CONTROL_CLASS} placeholder="Reference or keyword" value={search} onChange={(event) => setSearch(event.target.value)} />
      </div>

      <div className='flex flex-col gap-1.5 min-w-[170px]'>
        <label htmlFor="filter-category" className={LABEL}>
          Category
        </label>
        <select id="filter-category" className={CONTROL_CLASS} value={values.categoryId} onChange={(event) => onChange({ categoryId: event.target.value })}>
          <option value="">All categories</option>
          {(categories || []).map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </div>

      <div className='flex flex-col gap-1.5 min-w-[170px]'>
        <label htmlFor="filter-status" className={LABEL}>
          Status
        </label>
        <select id="filter-status" className={CONTROL_CLASS} value={values.status} onChange={(event) => onChange({ status: event.target.value })}>
          <option value="">All statuses</option>
          {STATUS_ORDER.map((status) => (
            <option key={status} value={status}>
              {statusLabel(status)}
            </option>
          ))}
        </select>
      </div>

      <div className='flex flex-col gap-1.5 min-w-[170px]'>
        <label htmlFor="filter-sort" className={LABEL}>
          Sort by age
        </label>
        <select id="filter-sort" className={CONTROL_CLASS} value={values.sort} onChange={(event) => onChange({ sort: event.target.value })}>
          <option value="age_desc">Oldest first</option>
          <option value="age_asc">Newest first</option>
        </select>
      </div>

      {active && (
        <Button
          variant="secondary"
          onClick={() => {
            setSearch('');
            onReset();
          }}
        >
          Clear filters
        </Button>
      )}
    </form>
  );
};

export default RequestFilterBar;
