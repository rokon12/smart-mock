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
        
        const response = await fetch('/admin/spec', {
            method: 'POST',
            headers: {
                'Content-Type': contentType
            },
            body: content
        });
        
        if (response.ok) {
            showUploadAlert('Specification uploaded successfully! Reloading...', 'success');
            setTimeout(() => {
                window.location.reload();
            }, 1500);
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
        // Load the sample spec using the dedicated endpoint
        const response = await fetch('/admin/spec/sample', {
            method: 'POST'
        });
        
        if (response.ok) {
            showUploadAlert('Sample specification loaded successfully! Reloading...', 'success');
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

// Clear current spec
async function clearSpec() {
    if (confirm('Are you sure you want to clear the current specification?')) {
        try {
            const response = await fetch('/admin/spec', {
                method: 'DELETE'
            });
            
            if (response.ok) {
                showNotification('Specification cleared successfully', 'success');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            } else {
                const error = await response.text();
                showNotification(`Failed to clear specification: ${error}`, 'danger');
            }
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
        const response = await fetch('/admin/spec');
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
        const response = await fetch('/admin/spec');
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

// Check OpenAPI spec status
function checkSpecStatus() {
    // Only run if we're on the home page
    if (window.location.pathname !== '/') return;
    
    // Check every 30 seconds if spec is not loaded
    const specLoadedElement = document.querySelector('.alert-success');
    if (!specLoadedElement) {
        setTimeout(() => {
            fetch('/admin/spec', { method: 'GET' })
                .then(response => {
                    if (response.ok) {
                        // Spec is now loaded, refresh the page
                        window.location.reload();
                    }
                })
                .catch(err => console.log('Spec check failed:', err));
            
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

// Export functions for global use
window.smartMock = {
    copyCode,
    testEndpoint,
    formatJSON,
    showNotification
};