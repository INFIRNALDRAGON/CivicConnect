import React from 'react';
import BrandMark from '../common/BrandMark';
import LifecycleTracker from '../common/LifecycleTracker';

const AuthLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <div className='min-h-screen grid md:grid-cols-[5fr_6fr]'>
      {/* Pitch panel */}
      <div className='flex flex-col justify-between gap-6 md:gap-10 p-6 md:px-14 md:py-10 bg-ink text-white'>
        <div className='flex items-center gap-3 font-display font-extrabold text-2xl'>
          <BrandMark size={36} />
          <span>CivicConnect</span>
        </div>
        <div className='md:mt-auto'>
          <h1 className='max-w-none md:max-w-[14ch] text-3xl md:text-5xl leading-[1.08]'>Tell us what needs fixing. Follow it until it is done.</h1>
          <p className='max-w-[38ch] mt-4 text-slate-300'>Every request gets a reference number and a visible status, from the moment you submit it to the day it is closed.</p>
        </div>
        <div className='hidden md:block' aria-hidden="true">
          <LifecycleTracker status="IN_PROGRESS" dark />
        </div>
      </div>

      {/* Form panel */}
      <div className='flex items-center justify-center px-5 py-10'>
        <div className='w-full max-w-[440px]'>{children}</div>
      </div>
    </div>
  );
};

export default AuthLayout;
