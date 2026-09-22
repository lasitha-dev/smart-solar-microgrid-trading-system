/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Interactive facility location map modal displaying prosumer node coordinates with OpenStreetMap.
 */

import React from 'react';
import { X, MapPin, ExternalLink, Compass, ShieldCheck } from 'lucide-react';

export const LocationMapModal = ({ isOpen, onClose, user }) => {
  if (!isOpen || !user) return null;

  const lat = user.latitude !== null && user.latitude !== undefined ? user.latitude : 6.9271;
  const lon = user.longitude !== null && user.longitude !== undefined ? user.longitude : 79.8612;
  const hasCoordinates = user.latitude !== null && user.latitude !== undefined;

  // OpenStreetMap embed coordinates bounding box (approx 0.01 degree span)
  const delta = 0.008;
  const bbox = `${lon - delta},${lat - delta},${lon + delta},${lat + delta}`;
  const osmEmbedUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${lat},${lon}`;
  const googleMapsUrl = `https://www.google.com/maps?q=${lat},${lon}`;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" style={{ maxWidth: '680px' }} onClick={(e) => e.stopPropagation()}>
        {/* Modal Header */}
        <div className="card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div style={{
              background: 'var(--solar-amber-light)',
              color: 'var(--solar-amber)',
              padding: '0.5rem',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <MapPin size={22} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.15rem' }}>Facility Location & Coordinates</h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                {user.fullName || user.username} ({user.nic})
              </p>
            </div>
          </div>
          <button className="btn btn-outline btn-sm" onClick={onClose} style={{ border: 'none', padding: '0.35rem' }}>
            <X size={20} />
          </button>
        </div>

        {/* Modal Body */}
        <div className="card-body" style={{ padding: '1.25rem' }}>
          {/* Details Bar */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '1fr auto',
            gap: '1rem',
            background: 'var(--bg-main)',
            border: '1px solid var(--border-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '0.9rem 1.1rem',
            marginBottom: '1rem',
            alignItems: 'center'
          }}>
            <div>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', fontWeight: 600 }}>
                Street / Facility Address
              </p>
              <p style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-main)' }}>
                {user.address || 'Address not registered'}
              </p>
            </div>

            <div style={{ textAlign: 'right' }}>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', fontWeight: 600 }}>
                Confirmed Coordinates
              </p>
              <div style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.5rem',
                fontFamily: 'monospace',
                fontSize: '0.85rem',
                color: 'var(--solar-amber)',
                fontWeight: 700,
                background: 'rgba(245, 158, 11, 0.1)',
                padding: '0.2rem 0.6rem',
                borderRadius: 'var(--radius-sm)'
              }}>
                <Compass size={14} />
                <span>{lat.toFixed(5)}° N, {lon.toFixed(5)}° E</span>
              </div>
            </div>
          </div>

          {/* Map Canvas / Embed Frame */}
          <div style={{
            position: 'relative',
            width: '100%',
            height: '340px',
            borderRadius: 'var(--radius-md)',
            overflow: 'hidden',
            border: '1px solid var(--border-light)',
            background: '#1a233a'
          }}>
            <iframe
              title="Facility Location Map"
              width="100%"
              height="100%"
              frameBorder="0"
              scrolling="no"
              marginHeight="0"
              marginWidth="0"
              src={osmEmbedUrl}
              style={{ border: 0 }}
            />
            
            {!hasCoordinates && (
              <div style={{
                position: 'absolute',
                bottom: '10px',
                left: '10px',
                background: 'rgba(15, 23, 42, 0.9)',
                color: 'var(--solar-amber)',
                padding: '0.4rem 0.8rem',
                borderRadius: 'var(--radius-sm)',
                fontSize: '0.75rem',
                border: '1px solid var(--border-subtle)'
              }}>
                * Default cluster location shown (User coordinates not locked)
              </div>
            )}
          </div>
        </div>

        {/* Modal Footer */}
        <div style={{
          padding: '0.9rem 1.25rem',
          background: 'var(--bg-card-header)',
          borderTop: '1px solid var(--border-subtle)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '0.75rem'
        }}>
          <div>
            <a
              href={googleMapsUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-outline btn-sm"
              style={{ fontSize: '0.8rem' }}
            >
              <ExternalLink size={14} />
              <span>Open in Google Maps</span>
            </a>
          </div>

          <button type="button" className="btn btn-outline btn-sm" onClick={onClose}>
            Close Map
          </button>
        </div>
      </div>
    </div>
  );
};
