"use client";

import { useState, useEffect, useRef } from 'react';
import { useNavigate } from '@/lib/navigation';
import { domToPng } from 'modern-screenshot';

// Import assets from the polaroid folder
import imgLogo from '../../assets/logo.png';
import InfoButton from '../shared/InfoButton';
import Tooltip from '../shared/Tooltip';
import { useExperimentProject } from '../../hooks/useExperimentProject';
import { Close } from '../icons/Close';


// Default project info (fallback if Sanity fetch fails)
const DEFAULT_POLAROID_PROJECT = {
  id: 'polaroid',
  title: 'Polaroid Studio',
  year: '2025',
  description: 'A digital way to customize your own polaroid.',
  imageSrc: 'https://image.mux.com/XJFJ1P3u9pKsFYvH9lTtOp4gPRydSpMkRrX9dRmNE5w/thumbnail.png',
  videoSrc: 'https://stream.mux.com/XJFJ1P3u9pKsFYvH9lTtOp4gPRydSpMkRrX9dRmNE5w.m3u8',
  xLink: 'https://x.com/michelletliu/status/1991201412072734777',
  tryItOutHref: '/polaroid',
  // 线上版实际底色为 sky-50（代码兜底 #eff6ff 不会走到）
  backgroundColor: '#f0f9ff',
  toolCategories: [
    { label: 'Design', tools: ['Figma'] },
    { label: 'Frontend', tools: ['TypeScript', 'React', 'Vite'] },
    { label: 'Styling', tools: ['Tailwind CSS'] },
    { label: 'AI', tools: ['Figma Make', 'Cursor'] },
  ],
};

type ColorOption = {
  id: string;
  name: string;
  fill: string;
  fillHover: string;
  tint: string;
  border: string;
};


const colors: ColorOption[] = [
  { id: 'red', name: 'Red', fill: '#FF383C', fillHover: '#E32226', tint: 'rgba(255, 56, 60, 0.15)', border: '#f0c8c9' },
  { id: 'orange', name: 'Orange', fill: '#FF8D28', fillHover: '#E67A1C', tint: 'rgba(255, 141, 40, 0.15)', border: '#ffe0c8' },
  { id: 'yellow', name: 'Yellow', fill: '#FFCC00', fillHover: '#E6B800', tint: 'rgba(255, 204, 0, 0.15)', border: '#fff2cc' },
  { id: 'green', name: 'Green', fill: '#34C759', fillHover: '#2DB04C', tint: 'rgba(52, 199, 89, 0.15)', border: '#d4eedd' },
  { id: 'cyan', name: 'Cyan', fill: '#00C3D0', fillHover: '#00A8B5', tint: 'rgba(0, 195, 208, 0.1)', border: '#c5f0e7' },
  { id: 'blue', name: 'Blue', fill: '#0088FF', fillHover: '#0070D9', tint: 'rgba(0, 136, 255, 0.15)', border: '#cce5ff' },
  { id: 'purple', name: 'Purple', fill: '#6155F5', fillHover: '#4E3FE0', tint: 'rgba(97, 85, 245, 0.15)', border: '#d1d0e0' },
];

function ColorButton({ 
  color, 
  isSelected, 
  onClick 
}: { 
  color: ColorOption; 
  isSelected: boolean; 
  onClick: () => void;
}) {
  const [isHovered, setIsHovered] = useState(false);

  if (isSelected) {
    return (
      <Tooltip label={color.name} position="top">
        <button
          onClick={onClick}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}
          className="relative shrink-0 size-[30px] cursor-pointer group"
          aria-label={`Select ${color.name} color`}
        >
          <div className="absolute inset-[-20%]">
            <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 42 42">
              <g>
                <circle 
                  cx="21" 
                  cy="21" 
                  fill={isHovered ? color.fillHover : color.fill} 
                  opacity="0.7" 
                  r="15" 
                  className="transition-all duration-200" 
                />
                <circle cx="21" cy="21" opacity="0.7" r="19.5" stroke="#0088FF" strokeWidth="3" />
              </g>
            </svg>
          </div>
        </button>
      </Tooltip>
    );
  }

  return (
    <Tooltip label={color.name} position="top">
      <button
        onClick={onClick}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
        className="relative shrink-0 size-[30px] cursor-pointer group"
        aria-label={`Select ${color.name} color`}
      >
        <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 30 30">
          <g opacity="0.7">
            <circle 
              cx="15" 
              cy="15" 
              fill={isHovered ? color.fillHover : color.fill} 
              r="15" 
              className="transition-all duration-200" 
            />
          </g>
        </svg>
      </button>
    </Tooltip>
  );
}

export default function PolaroidPage() {
  const navigate = useNavigate();
  
  // Fetch project info from Sanity (with fallback to defaults)
  const projectInfo = useExperimentProject('polaroid', DEFAULT_POLAROID_PROJECT);
  
  const [selectedColor, setSelectedColor] = useState<ColorOption | null>(null);
  const [showDate, setShowDate] = useState(false);
  const [showText, setShowText] = useState(false);
  const [caption, setCaption] = useState('');
  const [isEditingCaption, setIsEditingCaption] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(false);
  const [isDateFocused, setIsDateFocused] = useState(false);
  const [isTextFocused, setIsTextFocused] = useState(false);
  const [showUploadOverlay, setShowUploadOverlay] = useState(false);
  const [hoverTimer, setHoverTimer] = useState<NodeJS.Timeout | null>(null);
  const [isImageHovered, setIsImageHovered] = useState(false);
  const [uploadedImage, setUploadedImage] = useState<string | null>(null);
  const [isFileDialogOpen, setIsFileDialogOpen] = useState(false);
  const [isExiting, setIsExiting] = useState(false);
  const [isEntering, setIsEntering] = useState(true);
  const [isPopupMode, setIsPopupMode] = useState(false);
  const logoRef = useRef<HTMLButtonElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Detect if we're in popup mode (embedded in ExperimentModal)
  useEffect(() => {
    const checkPopupMode = () => {
      if (containerRef.current) {
        const isInModal = containerRef.current.closest('.experiment-modal-embed:not(.fullscreen)') !== null;
        setIsPopupMode(isInModal);
      }
    };
    checkPopupMode();
    
    // Re-check on resize in case modal state changes
    window.addEventListener('resize', checkPopupMode);
    
    // Watch for class changes on the modal embed (for fullscreen transitions)
    const modalEmbed = containerRef.current?.closest('.experiment-modal-embed');
    let observer: MutationObserver | null = null;
    if (modalEmbed) {
      observer = new MutationObserver(() => {
        checkPopupMode();
      });
      observer.observe(modalEmbed, { attributes: true, attributeFilter: ['class'] });
    }
    
    return () => {
      window.removeEventListener('resize', checkPopupMode);
      observer?.disconnect();
    };
  }, []);

  // Handle entrance animation
  useEffect(() => {
    // Small delay to ensure initial state renders first
    const timer = setTimeout(() => {
      setIsEntering(false);
    }, 50);
    return () => clearTimeout(timer);
  }, []);

  // Handle navigation back to home (or to popup mode if in fullscreen)
  const handleBackToHome = () => {
    // If we're in fullscreen mode, go to popup mode immediately (no exit animation)
    const isFullscreen = window.location.pathname.includes('/full');
    if (isFullscreen) {
      navigate('/project/polaroid');
      return;
    }
    
    // Otherwise animate exit then go to home
    setIsExiting(true);
    setTimeout(() => {
      navigate('/');
    }, 280);
  };

  // Parse URL params on mount to restore shared polaroid state
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    
    const colorId = params.get('color');
    if (colorId) {
      const color = colors.find(c => c.id === colorId);
      if (color) setSelectedColor(color);
    }
    
    const captionParam = params.get('caption');
    if (captionParam) {
      setCaption(decodeURIComponent(captionParam));
      setShowText(true);
    }
    
    const dateParam = params.get('date');
    if (dateParam === 'true') {
      setShowDate(true);
    }
  }, []);
  const polaroidRef = useRef<HTMLDivElement>(null);

  // Handle window focus to detect when file dialog is closed (including cancel)
  useEffect(() => {
    const handleWindowFocus = () => {
      if (isFileDialogOpen) {
        // Small delay to let onChange fire first if a file was selected
        setTimeout(() => {
          setIsFileDialogOpen(false);
          setShowUploadOverlay(false);
        }, 100);
      }
    };

    window.addEventListener('focus', handleWindowFocus);
    return () => window.removeEventListener('focus', handleWindowFocus);
  }, [isFileDialogOpen]);

  const currentDate = new Date().toLocaleDateString('en-US', {
    month: '2-digit',
    day: '2-digit',
    year: '2-digit'
  }).replace(/\//g, '.');

  const handleRestart = () => {
    setSelectedColor(null);
    setShowDate(false);
    setShowText(false);
    setCaption('');
    setIsEditingCaption(false);
    setUploadedImage(null);
  };

  const handleDownload = async () => {
    if (!polaroidRef.current) return;
    
    try {
      // Use modern-screenshot to capture the polaroid as PNG
      const dataUrl = await domToPng(polaroidRef.current, {
        scale: 3, // Higher resolution for better quality
        backgroundColor: null, // Transparent background for rounded corners
      });
      
      // Create download link with filename PolaroidStudio[Date].png
      const link = document.createElement('a');
      link.href = dataUrl;
      link.download = `PolaroidStudio${currentDate}.png`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      setDownloadSuccess(true);
      setTimeout(() => setDownloadSuccess(false), 2000);
    } catch (error) {
      console.error('Failed to download polaroid:', error);
    }
  };





  const handleImageHoverStart = () => {
    setIsImageHovered(true);
    // Only show upload overlay if there's already an uploaded image
    if (uploadedImage) {
      const timer = setTimeout(() => {
        setShowUploadOverlay(true);
      }, 50); // Faster delay
      setHoverTimer(timer);
    }
  };

  const handleImageHoverEnd = () => {
    setIsImageHovered(false);
    if (hoverTimer) {
      clearTimeout(hoverTimer);
      setHoverTimer(null);
    }
    // Don't hide overlay if file dialog is open
    if (!isFileDialogOpen) {
      setShowUploadOverlay(false);
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Validate that the file is an image
      if (!file.type.startsWith('image/')) {
        alert('Please upload an image file (e.g., .jpg, .png, .gif)');
        setIsFileDialogOpen(false);
        setShowUploadOverlay(false);
        return;
      }
      
      const reader = new FileReader();
      reader.onload = (event) => {
        setUploadedImage(event.target?.result as string);
        setIsFileDialogOpen(false);
        setShowUploadOverlay(false);
        // Reset input value so the same file can be re-uploaded
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      };
      reader.onerror = () => {
        console.error('Error reading file');
        alert('Error reading file. Please try again.');
        setIsFileDialogOpen(false);
        setShowUploadOverlay(false);
      };
      reader.readAsDataURL(file);
    }
  };

  return (
    <>
      {/* Logo - fixed outside transitioning container, animates in smoothly */}
      <button
        ref={logoRef}
        onClick={handleBackToHome}
        className={`fixed top-8 left-6 md:left-16 z-40 cursor-pointer transition-all duration-300 ease-out hover:opacity-80 ${
          isEntering ? 'opacity-0 translate-y-2' : 'opacity-100 translate-y-0'
        }`}
        aria-label="Go back to home"
      >
        <img 
          src={imgLogo} 
          alt="Michelle Liu Logo" 
          className="w-8 h-8 md:w-[44px] md:h-[44px] object-contain"
        />
      </button>

      {/* Info Button - fixed top right */}
      <InfoButton project={projectInfo} />

      <div 
        ref={containerRef}
        className={`polaroid-page-container relative w-full px-4 flex flex-col items-center transition-all ${
          isPopupMode ? 'h-full justify-center' : 'min-h-screen min-h-[100dvh]'
        } ${
          isExiting ? 'opacity-0 scale-[0.985]' : isEntering ? 'opacity-0 scale-[1.01]' : 'opacity-100 scale-100'
        }`}
        style={{ 
          backgroundColor: projectInfo.backgroundColor || '#eff6ff',
          transitionDuration: isExiting ? '280ms' : '300ms',
          transitionTimingFunction: isExiting ? 'cubic-bezier(0.4, 0, 0.2, 1)' : 'ease-out'
        }}
        onClick={() => {
          setIsDateFocused(false);
          setIsTextFocused(false);
        }}
      >

      <div className={`flex flex-col items-center w-full max-w-[583px] my-auto ${isPopupMode ? 'gap-[20px] md:gap-[28px] pt-[60px] pb-[40px] md:pt-[70px] md:pb-[50px]' : 'gap-[32px] md:gap-[48px] py-[100px] md:py-[120px]'}`}>
        {/* Title */}
        <div 
          className="flex flex-col font-['SF_Pro:Regular',sans-serif] font-normal justify-center relative shrink-0 text-2xl md:text-3rxl text-zinc-900 text-center text-nowrap" 
          style={{ fontVariationSettings: "'wdth' 100" }}
        >
          <p className="">
            <span>Polaroid </span>
            <span className="text-zinc-400">Studio</span>
          </p>
        </div>

        {/* Polaroid Frame */}
        <div 
          ref={polaroidRef}
          className={`content-stretch flex items-center justify-center relative rounded-sm md:rounded-md shadow-media shrink-0 transition-transform duration-300 ease-out hover:rotate-2 ${
            isPopupMode 
              ? 'h-[222px] md:h-[274px] w-[192px] md:w-[236px]' 
              : 'h-[320px] md:h-[393.22px] w-[274px] md:w-[337.288px]'
          }`}>
          <div className={`relative rounded-sm md:rounded-md shrink-0 ${
            isPopupMode 
              ? 'h-[222px] md:h-[274px] w-[192px] md:w-[236px]' 
              : 'h-[320px] md:h-[393.22px] w-[274px] md:w-[337.288px]'
          }`}>
            <div className="absolute contents left-0 top-0">
              <div className="absolute contents left-0 top-0">
                <div 
                  className="absolute blur-[0.25px] filter h-full left-0 rounded-[3.5px] md:rounded-[6.78px] top-0 w-full"
                  style={{ 
                    backgroundColor: selectedColor ? selectedColor.tint : 'white',
                  }}
                >
                  <div 
                    aria-hidden="true" 
                    className="absolute border border-zinc-200/80 border-solid inset-[-1px] pointer-events-none rounded-[4.5px] md:rounded-[8.475px]"
                    style={{
                      borderColor: selectedColor ? selectedColor.border : undefined
                    }}
                  />
                </div>
              </div>
              <div 
                className={`absolute content-stretch flex items-center left-0 rounded-[3.5px] md:rounded-[6.78px] top-0 ${
                  isPopupMode 
                    ? 'pb-[40px] md:pb-[50px] pt-[14px] md:pt-[16px] px-[12px] md:px-[14px]' 
                    : 'pb-[62px] md:pb-[76.271px] pt-[16.5px] md:pt-[20.339px] px-[16.5px] md:px-[20.339px]'
                }`}
                style={{ 
                  backgroundColor: selectedColor ? selectedColor.tint : 'white',
                }}
              >
                <div 
                  className={`relative rounded-[1.75px] md:rounded-[3.39px] shrink-0 ${
                    isPopupMode 
                      ? 'w-[168px] h-[168px] md:w-[208px] md:h-[208px]' 
                      : 'w-[241px] h-[241px] md:w-[296.61px] md:h-[296.61px]'
                  }`}
                  onMouseEnter={handleImageHoverStart}
                  onMouseLeave={handleImageHoverEnd}
                >
                  <div className="overflow-clip relative rounded-[inherit] size-full">
                    {uploadedImage ? (
                      /* Uploaded image - preserve aspect ratio and center.
                         A 20% white overlay softens the polaroid tint that
                         would otherwise show through letterbox bars on
                         non-square photos. */
                      <div className="absolute inset-0 flex items-center justify-center overflow-hidden rounded-sm md:rounded-md bg-white/20">
                        <img 
                          alt="Polaroid photo" 
                          className="max-w-full max-h-full w-auto h-auto object-contain" 
                          src={uploadedImage} 
                        />
                      </div>
                    ) : (
                      /* Default placeholder - light gray background with centered circle and upload icon */
                      /* Darkens on hover to indicate clickability */
                      <button
                        type="button"
                        onClick={() => {
                          setIsFileDialogOpen(true);
                          fileInputRef.current?.click();
                        }}
                        className="absolute inset-0 rounded-sm md:rounded-md flex items-center justify-center cursor-pointer transition-colors duration-200"
                        style={{ backgroundColor: isImageHovered ? '#e4e4e7' : '#f4f4f5' }}
                      >
                        <div 
                          className={`rounded-full flex items-center justify-center transition-colors duration-200 ${
                            isPopupMode 
                              ? 'w-[110px] h-[110px] md:w-[145px] md:h-[145px]' 
                              : 'w-[162px] h-[162px] md:w-[200px] md:h-[200px]'
                          }`}
                          style={{ backgroundColor: isImageHovered ? '#d4d4d8' : '#e4e4e7' }}
                        >
                          <svg 
                            width={isPopupMode ? "40" : "65"}
                            height={isPopupMode ? "40" : "65"}
                            viewBox="0 0 24 24" 
                            fill="none" 
                            stroke={isImageHovered ? '#71717a' : '#a1a1aa'} 
                            strokeWidth="1.5" 
                            strokeLinecap="round" 
                            strokeLinejoin="round" 
                            className={`transition-colors duration-200 ${isPopupMode ? 'md:w-[52px] md:h-[52px]' : 'md:w-[80px] md:h-[80px]'}`}
                          >
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                            <polyline points="17 8 12 3 7 8" />
                            <line x1="12" y1="3" x2="12" y2="15" />
                          </svg>
                        </div>
                      </button>
                    )}
                  </div>
                  
                  {/* Hidden file input - always in DOM so onChange fires properly */}
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleFileUpload}
                    className="sr-only"
                    aria-hidden="true"
                  />
                  
                  {/* Upload Overlay - only shows when there's an uploaded image */}
                  {showUploadOverlay && uploadedImage && (
                    <>
                      <div className="absolute bg-[rgba(196,196,196,0.5)] inset-0 rounded-[3.39px] transition-all duration-150 ease-out animate-fade-in-fast" />
                      <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-10">
                        <button 
                          type="button"
                          onClick={() => {
                            setIsFileDialogOpen(true);
                            fileInputRef.current?.click();
                          }}
                          className="cursor-pointer block transition-all duration-150 ease-out animate-scale-in-fast"
                        >
                          <div className="bg-zinc-50 hover:!bg-zinc-200 content-stretch flex gap-[12px] items-center px-6 py-3 rounded-[999px] shadow-soft hover:shadow-media transition-all duration-300 ease-in-out">
                            <div aria-hidden="true" className="absolute border border-zinc-200/50 border-solid inset-0 pointer-events-none rounded-[999px]" />
                            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="rgba(60,60,67,0.6)" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                              <polyline points="17 8 12 3 7 8" />
                              <line x1="12" y1="3" x2="12" y2="15" />
                            </svg>
                            <span className="font-['SF_Pro','-apple-system',sans-serif] text-lg text-[rgba(60,60,67,0.6)] whitespace-nowrap">
                              Upload New
                            </span>
                          </div>
                        </button>
                      </div>
                    </>
                  )}
                  
                  <div 
                    aria-hidden="true" 
                    className="absolute border-[0.847px] border-solid border-zinc-200/50 inset-0 pointer-events-none rounded-[3.39px]"
                    style={{
                      borderColor: selectedColor ? `${selectedColor.fill}4D` : undefined
                    }}
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Date Text */}
          {showDate && (
            <div 
              className={`absolute flex flex-col justify-center leading-[0] not-italic text-[rgba(0,0,0,0.4)] text-center text-nowrap tracking-[0.15em] -translate-x-1/2 -translate-y-1/2 ${
                isPopupMode 
                  ? 'text-xs md:text-xs left-[161px] md:left-[198px] top-[211px] md:top-[259px]' 
                  : 'text-sm md:text-base left-[229.7px] md:left-[282.7px] top-[301.5px] md:top-[370.72px]'
              }`}
              style={{ fontFamily: "'Courier New', monospace" }}
            >
              <p className="leading-6 md:leading-7">{currentDate}</p>
            </div>
          )}

          {/* Caption Text Box */}
          {showText && (
            <div 
              className={`absolute content-stretch flex items-center px-[6.5px] md:px-[8px] py-[3.25px] md:py-[4px] ${
                isPopupMode 
                  ? 'left-[5.5px] md:left-[6px] right-[5.5px] md:right-[6px] top-[182px] md:top-[224px]' 
                  : 'left-[10px] md:left-[12.3px] right-[10px] md:right-[12.3px] top-[260.2px] md:top-[320.29px]'
              }`}
            >
              <div className="content-stretch flex items-center relative w-full overflow-hidden">
                <div 
                  className={`flex flex-col justify-center leading-[0] not-italic relative text-zinc-900 text-center text-nowrap tracking-[0.15em] overflow-hidden text-ellipsis ${
                    isPopupMode ? 'text-xs md:text-xs' : 'text-sm md:text-lg'
                  }`}
                  style={{ fontFamily: "'Courier New', monospace" }}
                >
                  <p className={isPopupMode ? 'leading-4 md:leading-5' : 'leading-6 md:leading-7'}>{caption}</p>
                </div>
                {isEditingCaption && (
                  <div className={`relative shrink-0 animate-blink ${
                    isPopupMode ? 'h-[16px] md:h-[20px] w-[1.2px] md:w-[1.5px]' : 'h-[24.4px] md:h-[30px] w-[1.6px] md:w-[2px]'
                  }`}>
                    <svg className="block size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 2 30">
                      <path d="M1 2L1 28" stroke="#0088FF" strokeLinecap="round" strokeWidth="2" />
                    </svg>
                  </div>
                )}
                <input
                  type="text"
                  value={caption}
                  onChange={(e) => setCaption(e.target.value)}
                  onFocus={() => setIsEditingCaption(true)}
                  onBlur={() => {
                    setIsEditingCaption(false);
                    if (caption.trim() === '') {
                      setShowText(false);
                      setIsTextFocused(false);
                    }
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      setIsTextFocused(false);
                      e.currentTarget.blur();
                    }
                  }}
                  placeholder=""
                  className="absolute inset-0 opacity-0 cursor-text"
                  style={{ width: '300px' }}
                  maxLength={isPopupMode ? 28 : 30}
                />
              </div>
            </div>
          )}
        </div>

        {/* Controls */}
        <div className={`content-stretch flex flex-col items-center relative shrink-0 ${isPopupMode ? 'gap-[20px] md:gap-[28px]' : 'gap-[36px]'}`}>
          {/* Toolbar */}
          <div className="content-stretch flex items-center px-[8px] py-0 relative rounded-2xl shrink-0 w-full justify-center">
            <div className="content-stretch flex md:flex-row flex-col gap-[10px] items-center justify-center relative shrink-0">
              {/* Color Palette */}
              <div className="bg-white content-stretch flex items-center md:p-[10px] p-[8px] relative rounded-[1000px] shrink-0 shadow-soft overflow-visible">
                <div aria-hidden="true" className="absolute border border-zinc-200/80 border-solid inset-0 pointer-events-none rounded-[1000px]" />
                <div className="content-stretch flex md:gap-[18px] gap-[12px] items-center relative shrink-0 overflow-visible">
                  {colors.map((color) => (
                    <ColorButton
                      key={color.id}
                      color={color}
                      isSelected={selectedColor?.id === color.id}
                      onClick={() => setSelectedColor(color)}
                    />
                  ))}
                </div>
              </div>

              {/* Toggle Buttons Container */}
              <div className="flex gap-[10px] items-center">
                {/* Date Toggle Button */}
                <div className="bg-white content-stretch flex items-center p-[5px] relative rounded-[1000px] shrink-0 shadow-soft">
                  <div aria-hidden="true" className="absolute border border-zinc-200/80 border-solid inset-0 pointer-events-none rounded-[1000px]" />
                  <Tooltip label="Date" position="top" offset={1}>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setShowDate(!showDate);
                        if (!showDate) {
                          setIsDateFocused(true);
                          setIsTextFocused(false);
                        }
                      }}
                      className={`relative rounded-[999px] shrink-0 size-[40px] cursor-pointer transition-all duration-300 ${
                        showDate && isDateFocused ? 'bg-[#0088FF]' : showDate ? 'bg-[#e4e4e7] hover:bg-[#d4d4d8]' : 'bg-transparent hover:bg-[#e5e5e5]'
                      }`}
                      aria-label="Toggle date"
                    >
                      <div className={`absolute inset-0 flex items-center justify-center transition-colors duration-300`}>
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke={showDate && isDateFocused ? 'white' : showDate ? '#27272a' : '#18181b'} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                          <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                          <line x1="16" y1="2" x2="16" y2="6" />
                          <line x1="8" y1="2" x2="8" y2="6" />
                          <line x1="3" y1="10" x2="21" y2="10" />
                        </svg>
                      </div>
                    </button>
                  </Tooltip>
                </div>

                {/* Text Toggle Button */}
                <div className="bg-white content-stretch flex items-center p-[5px] relative rounded-[1000px] shrink-0 shadow-soft">
                  <div aria-hidden="true" className="absolute border border-zinc-200/80 border-solid inset-0 pointer-events-none rounded-[1000px]" />
                  <Tooltip label="Caption" position="top" offset={1}>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        if (showText) {
                          setShowText(false);
                          setIsTextFocused(false);
                        } else {
                          setShowText(true);
                          setIsTextFocused(true);
                          setIsDateFocused(false);
                          setTimeout(() => {
                            const input = document.querySelector('input[type="text"]') as HTMLInputElement;
                            if (input) input.focus();
                          }, 100);
                        }
                      }}
                      className={`relative rounded-[999px] shrink-0 size-[40px] cursor-pointer transition-all duration-300 ${
                        showText && isTextFocused ? 'bg-[#0088FF]' : showText ? 'bg-[#e4e4e7] hover:bg-[#d4d4d8]' : 'bg-transparent hover:bg-[#e5e5e5]'
                      }`}
                      aria-label="Toggle text"
                    >
                      <div className={`absolute inset-0 flex items-center justify-center transition-colors duration-300`}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke={showText && isTextFocused ? 'white' : showText ? '#27272a' : '#18181b'} strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="4 7 4 4 20 4 20 7" />
                          <line x1="9" y1="20" x2="15" y2="20" />
                          <line x1="12" y1="4" x2="12" y2="20" />
                        </svg>
                      </div>
                    </button>
                  </Tooltip>
                </div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="content-stretch flex gap-[24px] items-center relative shrink-0">
            <button 
              className="flex items-center gap-[8px] px-6 py-3 text-lg text-[rgba(60,60,67,0.5)] cursor-pointer rounded-[999px] hover:bg-[rgba(0,0,0,0.05)] transition-colors"
              onClick={handleRestart}
            >
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
                <path d="M3 3v5h5" />
              </svg>
              <span className="font-['SF_Pro','-apple-system',sans-serif]">Restart</span>
            </button>

            <button 
              className={`relative rounded-[999px] flex items-center gap-[8px] px-6 py-3 text-lg text-white transition-colors ${
                uploadedImage 
                  ? 'bg-zinc-900 cursor-pointer hover:bg-[rgba(24,24,27,0.8)]' 
                  : 'bg-zinc-800 cursor-not-allowed opacity-50'
              }`}
              onClick={handleDownload}
              disabled={!uploadedImage}
            >
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="transition-all duration-200" style={{ opacity: downloadSuccess ? 0 : 1 }}>
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" className="absolute transition-all duration-200" style={{ opacity: downloadSuccess ? 1 : 0 }}>
                <polyline points="20 6 9 17 4 12" />
              </svg>
              <span className="font-['SF_Pro','-apple-system',sans-serif]">{downloadSuccess ? 'Saved!' : 'Download'}</span>
            </button>
          </div>
        </div>
      </div>


      {/* Styles for animations */}
      <style>{`
        @keyframes blink {
          0%, 49% { opacity: 1; }
          50%, 100% { opacity: 0; }
        }
        .animate-blink {
          animation: blink 1s infinite;
        }
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes scaleInCentered {
          from { 
            opacity: 0; 
            transform: scale(0.9);
          }
          to { 
            opacity: 1; 
            transform: scale(1);
          }
        }
        .animate-fade-in {
          animation: fadeIn 0.4s ease-in-out forwards;
        }
        .animate-scale-in-centered {
          animation: scaleInCentered 0.4s ease-in-out forwards;
        }
        .animate-fade-in-fast {
          animation: fadeIn 0.15s ease-out forwards;
        }
        .animate-scale-in-fast {
          animation: scaleInCentered 0.15s ease-out forwards;
        }
        .scrollbar-hide::-webkit-scrollbar {
          display: none;
        }
        .scrollbar-hide {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
        /* Polaroid page container - enable scrolling when content overflows */
        .polaroid-page-container {
          overflow-y: auto;
        }
      `}</style>
      </div>
    </>
  );
}

