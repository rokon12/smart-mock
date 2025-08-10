// Smart Mock Server - JavaScript functionality

// Handle spec file upload
document.addEventListener('DOMContentLoaded', function() {
    const uploadForm = document.getElementById('uploadForm');
    if (uploadForm) {
        uploadForm.addEventListener('submit', handleSpecUpload);
    }
});

// Upload OpenAPI specification
async function handleSpecUpload(event) {
    event.preventDefault();
    
    const fileInput = document.getElementById('specFile');
    const file = fileInput.files[0];
    const alertDiv = document.getElementById('uploadAlert');
    
    if (!file) {
        showUploadAlert('Please select a file', 'danger');
        return;
    }
    
    // Show loading state
    showUploadAlert('Uploading specification...', 'info');
    const submitBtn = event.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Uploading...';
    
    try {
        const content = await file.text();
        const contentType = file.name.endsWith('.json') ? 'application/json' : 'application/yaml';
        
        // Extract name from filename (remove extension)
        const name = file.name.replace(/\.(yaml|yml|json)$/i, '');
        
        // Use the new schema API endpoint
        const response = await fetch(`/api/schemas?name=${encodeURIComponent(name)}`, {
            method: 'POST',
            headers: {
                'Content-Type': contentType
            },
            body: content
        });
        
        if (response.ok) {
            const result = await response.json();
            showUploadAlert(`Specification "${name}" uploaded successfully! Schema ID: ${result.id}`, 'success');
            
            // Clear the file input
            fileInput.value = '';
            
            // Reset button
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
            
            // Reload after a short delay
            setTimeout(() => {
                window.location.reload();
            }, 2000);
        } else {
            const error = await response.text();
            showUploadAlert(`Upload failed: ${error}`, 'danger');
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    } catch (error) {
        showUploadAlert(`Error: ${error.message}`, 'danger');
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
    }
}

// Load sample Pet Store spec
async function loadSampleSpec() {
    const alertDiv = document.getElementById('uploadAlert');
    showUploadAlert('Loading sample specification...', 'info');
    
    try {
        const response = await fetch('/api/schemas/load-samples', {
            method: 'POST'
        });
        
        if (response.ok) {
            const result = await response.json();
            showUploadAlert(`Sample specification loaded successfully! Loaded ${result.loaded} schema(s). Reloading...`, 'success');
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } else {
            const error = await response.text();
            showUploadAlert(`Failed to load sample: ${error}`, 'danger');
        }
    } catch (error) {
        showUploadAlert(`Error loading sample: ${error.message}`, 'danger');
    }
}

// Clear all schemas
async function clearSpec() {
    if (confirm('Are you sure you want to clear all schemas?')) {
        try {
            // Get list of all schemas
            const schemasResponse = await fetch('/api/schemas');
            if (!schemasResponse.ok) {
                showNotification('Failed to get schemas list', 'danger');
                return;
            }
            
            const schemas = await schemasResponse.json();
            
            // Delete each schema
            for (const schema of schemas) {
                await fetch(`/api/schemas/${schema.id}`, {
                    method: 'DELETE'
                });
            }
            
            showNotification('All schemas cleared successfully', 'success');
            setTimeout(() => {
                window.location.reload();
            }, 1500);
        } catch (error) {
            showNotification(`Error: ${error.message}`, 'danger');
        }
    }
}

// View full specification
async function viewSpec() {
    const modal = new bootstrap.Modal(document.getElementById('specModal'));
    const specContent = document.getElementById('specContent');
    
    specContent.textContent = 'Loading...';
    modal.show();
    
    try {
        const response = await fetch('/api-spec');
        if (response.ok) {
            const spec = await response.json();
            const yamlContent = JSON.stringify(spec, null, 2);
            specContent.textContent = yamlContent;
            
            // Re-highlight syntax
            if (window.Prism) {
                Prism.highlightElement(specContent);
            }
        } else {
            specContent.textContent = 'Failed to load specification';
        }
    } catch (error) {
        specContent.textContent = `Error: ${error.message}`;
    }
}

// Download specification
async function downloadSpec() {
    try {
        const response = await fetch('/api-spec');
        if (response.ok) {
            const spec = await response.json();
            const yamlContent = JSON.stringify(spec, null, 2);
            
            // Create blob and download
            const blob = new Blob([yamlContent], { type: 'application/json' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'openapi-spec.json';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            
            showNotification('Specification downloaded successfully', 'success');
        }
    } catch (error) {
        showNotification(`Download failed: ${error.message}`, 'danger');
    }
}

// Copy specification to clipboard
function copySpec() {
    const specContent = document.getElementById('specContent');
    const text = specContent.textContent;
    
    navigator.clipboard.writeText(text).then(() => {
        showNotification('Specification copied to clipboard', 'success');
    }).catch(err => {
        showNotification('Failed to copy to clipboard', 'danger');
    });
}

// Show upload alert
function showUploadAlert(message, type) {
    const alertDiv = document.getElementById('uploadAlert');
    if (alertDiv) {
        alertDiv.className = `alert alert-${type}`;
        alertDiv.textContent = message;
        alertDiv.classList.remove('d-none');
    }
}

// Copy code to clipboard
function copyCode(elementId) {
    const codeElement = document.getElementById(elementId);
    const button = event.target.closest('button');
    
    if (!codeElement) return;
    
    // Get the text content
    const text = codeElement.textContent || codeElement.innerText;
    
    // Copy to clipboard
    navigator.clipboard.writeText(text).then(() => {
        // Update button text and style
        const originalHTML = button.innerHTML;
        button.innerHTML = '<i class="bi bi-check"></i> Copied!';
        button.classList.add('copied');
        
        // Reset after 2 seconds
        setTimeout(() => {
            button.innerHTML = originalHTML;
            button.classList.remove('copied');
        }, 2000);
    }).catch(err => {
        console.error('Failed to copy text: ', err);
        alert('Failed to copy to clipboard');
    });
}

// Initialize tooltips
document.addEventListener('DOMContentLoaded', function() {
    // Bootstrap tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Event delegation for schema management buttons
    document.addEventListener('click', function(e) {
        if (e.target.closest('.btn-activate-schema')) {
            const btn = e.target.closest('.btn-activate-schema');
            const schemaId = btn.dataset.schemaId;
            activateSchema(schemaId);
        }
        
        if (e.target.closest('.btn-explore-schema')) {
            const btn = e.target.closest('.btn-explore-schema');
            const schemaId = btn.dataset.schemaId;
            exploreSchema(schemaId);
        }
        
        if (e.target.closest('.btn-delete-schema')) {
            const btn = e.target.closest('.btn-delete-schema');
            const schemaId = btn.dataset.schemaId;
            deleteSchema(schemaId);
        }
    });
    
    // Add smooth scroll behavior for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
    
    // Add interactive hover effects to cards
    const cards = document.querySelectorAll('.feature-card, .tech-item');
    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.cursor = 'pointer';
        });
    });
    
    // Auto-refresh spec status (optional)
    if (window.location.pathname === '/') {
        checkSpecStatus();
    }
});

// Check schema status
function checkSpecStatus() {
    // Only run if we're on the home page
    if (window.location.pathname !== '/') return;
    
    // Check every 30 seconds if no schema is active
    const specLoadedElement = document.querySelector('.alert-success');
    if (!specLoadedElement) {
        setTimeout(() => {
            fetch('/api/schemas')
                .then(response => {
                    if (response.ok) {
                        return response.json();
                    }
                })
                .then(schemas => {
                    if (schemas && schemas.length > 0) {
                        // Schemas are now loaded, refresh the page
                        window.location.reload();
                    }
                })
                .catch(err => console.log('Schema check failed:', err));
            
            // Check again
            checkSpecStatus();
        }, 30000); // 30 seconds
    }
}

// Format JSON response (utility function)
function formatJSON(jsonString) {
    try {
        const obj = JSON.parse(jsonString);
        return JSON.stringify(obj, null, 2);
    } catch (e) {
        return jsonString;
    }
}

// Add a simple API tester (optional enhancement)
function testEndpoint(method, path, headers = {}, body = null) {
    const baseUrl = window.location.origin;
    const url = `${baseUrl}/mock${path}`;
    
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            ...headers
        }
    };
    
    if (body && method !== 'GET') {
        options.body = JSON.stringify(body);
    }
    
    return fetch(url, options)
        .then(response => response.json())
        .then(data => {
            console.log('Response:', data);
            return data;
        })
        .catch(error => {
            console.error('Error:', error);
            throw error;
        });
}

// Utility: Display notification
function showNotification(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    alertDiv.style.zIndex = '9999';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(alertDiv);
    
    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        alertDiv.remove();
    }, 5000);
}

// Schema management functions
async function activateSchema(schemaId) {
    try {
        const response = await fetch(`/api/schemas/${schemaId}/activate`, {
            method: 'POST'
        });
        
        if (response.ok) {
            showNotification('Schema activated successfully', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showNotification('Failed to activate schema', 'danger');
        }
    } catch (error) {
        showNotification(`Error: ${error.message}`, 'danger');
    }
}

async function deleteSchema(schemaId) {
    if (confirm('Are you sure you want to delete this schema?')) {
        try {
            const response = await fetch(`/api/schemas/${schemaId}`, {
                method: 'DELETE'
            });
            
            if (response.ok) {
                showNotification('Schema deleted successfully', 'success');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                showNotification('Failed to delete schema', 'danger');
            }
        } catch (error) {
            showNotification(`Error: ${error.message}`, 'danger');
        }
    }
}

function exploreSchema(schemaId) {
    window.open(`/swagger-ui.html?schemaId=${schemaId}`, '_blank');
}

async function loadSampleSchemas() {
    try {
        const response = await fetch('/api/schemas/load-samples', {
            method: 'POST'
        });
        
        if (response.ok) {
            const result = await response.json();
            showNotification(`Loaded ${result.loaded} sample schemas`, 'success');
            setTimeout(() => window.location.reload(), 1500);
        } else {
            showNotification('Failed to load sample schemas', 'danger');
        }
    } catch (error) {
        showNotification(`Error: ${error.message}`, 'danger');
    }
}

// Export functions for global use
window.smartMock = {
    copyCode,
    testEndpoint,
    formatJSON,
    showNotification,
    activateSchema,
    deleteSchema,
    exploreSchema,
    loadSampleSchemas
};