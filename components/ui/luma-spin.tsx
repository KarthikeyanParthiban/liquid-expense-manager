"use client";

import React from "react";

export interface LumaSpinProps {
  size?: number;
  className?: string;
  colorClassName?: string;
}

export const LumaSpin: React.FC<LumaSpinProps> = ({
  size = 65,
  className = "",
  colorClassName = "shadow-gray-900 dark:shadow-gray-100",
}) => {
  const halfSize = Math.round(size * (35 / 65));

  return (
    <div
      className={`relative aspect-square ${className}`}
      style={{ width: `${size}px` }}
      role="status"
      aria-label="Loading"
    >
      <span
        className={`absolute rounded-[50px] animate-luma-spin shadow-[inset_0_0_0_3px] ${colorClassName}`}
      />
      <span
        className={`absolute rounded-[50px] animate-luma-spin animation-delay shadow-[inset_0_0_0_3px] ${colorClassName}`}
      />
      <style jsx>{`
        @keyframes loaderAnim {
          0% {
            inset: 0 ${halfSize}px ${halfSize}px 0;
          }
          12.5% {
            inset: 0 ${halfSize}px 0 0;
          }
          25% {
            inset: ${halfSize}px ${halfSize}px 0 0;
          }
          37.5% {
            inset: ${halfSize}px 0 0 0;
          }
          50% {
            inset: ${halfSize}px 0 0 ${halfSize}px;
          }
          62.5% {
            inset: 0 0 0 ${halfSize}px;
          }
          75% {
            inset: 0 0 ${halfSize}px ${halfSize}px;
          }
          87.5% {
            inset: 0 0 ${halfSize}px 0;
          }
          100% {
            inset: 0 ${halfSize}px ${halfSize}px 0;
          }
        }
        .animate-luma-spin {
          animation: loaderAnim 2.5s cubic-bezier(0.4, 0, 0.2, 1) infinite;
        }
        .animation-delay {
          animation-delay: -1.25s;
        }
      `}</style>
    </div>
  );
};

export const Component = LumaSpin;
