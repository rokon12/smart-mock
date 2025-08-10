document.addEventListener('DOMContentLoaded', function() {
    const uploadForm = document.getElementById('uploadForm');
    if (uploadForm) {
        uploadForm.addEventListener('submit', handleSpecUpload);
    }
});

async function handleSpecUpload(event) {
    event.preventDefault();
    
    const fileInput = document.getElementById('specFile');
    const file = fileInput.files[0];
    const alertDiv = document.getElementById('uploadAlert');
    
    if (!file) {
        showUploadAlert('Please select a file', 'danger');
        return;
    }
    
    showUploadAlert('Uploading specification...', 'info');
    const submitBtn = event.target.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Uploading...';
    
    try {
        const content = await file.text();
        const contentType = file.name.endsWith('.json') ? 'application/json' : 'application/yaml';
        
        const name = file.name.replace(/\.(yaml|yml|json)$/i, '');
        
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
            
            fileInput.value = '';
            
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
            
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

async function clearSpec() {
    if (confirm('Are you sure you want to clear all schemas?')) {
        try {
            const schemasResponse = await fetch('/api/schemas');
            if (!schemasResponse.ok) {
                showNotification('Failed to get schemas list', 'danger');
                return;
            }
            
            const schemas = await schemasResponse.json();
            
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

async function downloadSpec() {
    try {
        const response = await fetch('/api-spec');
        if (response.ok) {
            const spec = await response.json();
            const yamlContent = JSON.stringify(spec, null, 2);
            
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

function copySpec() {
    const specContent = document.getElementById('specContent');
    const text = specContent.textContent;
    
    navigator.clipboard.writeText(text).then(() => {
        showNotification('Specification copied to clipboard', 'success');
    }).catch(err => {
        showNotification('Failed to copy to clipboard', 'danger');
    });
}

function showUploadAlert(message, type) {
    const alertDiv = document.getElementById('uploadAlert');
    if (alertDiv) {
        alertDiv.className = `alert alert-${type}`;
        alertDiv.textContent = message;
        alertDiv.classList.remove('d-none');
    }
}

function copyCode(elementId) {
    const codeElement = document.getElementById(elementId);
    const button = event.target.closest('button');
    
    if (!codeElement) return;
    
    const text = codeElement.textContent || codeElement.innerText;
    
    navigator.clipboard.writeText(text).then(() => {
        const originalHTML = button.innerHTML;
        button.innerHTML = '<i class="bi bi-check"></i> Copied!';
        button.classList.add('copied');
        
        setTimeout(() => {
            button.innerHTML = originalHTML;
            button.classList.remove('copied');
        }, 2000);
    }).catch(err => {
        console.error('Failed to copy text: ', err);
        alert('Failed to copy to clipboard');
    });
}

document.addEventListener('DOMContentLoaded', function() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
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
    
    const cards = document.querySelectorAll('.feature-card, .tech-item');
    cards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.cursor = 'pointer';
        });
    });
    
    if (window.location.pathname === '/') {
        checkSpecStatus();
    }
});

function checkSpecStatus() {
    if (window.location.pathname !== '/') return;
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
                        window.location.reload();
                    }
                })
                .catch(err => console.log('Schema check failed:', err));
            
            checkSpecStatus();
        }, 30000);
    }
}

function formatJSON(jsonString) {
    try {
        const obj = JSON.parse(jsonString);
        return JSON.stringify(obj, null, 2);
    } catch (e) {
        return jsonString;
    }
}

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

function showNotification(message, type = 'info') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3`;
    alertDiv.style.zIndex = '9999';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    document.body.appendChild(alertDiv);
    
    setTimeout(() => {
        alertDiv.remove();
    }, 5000);
}

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