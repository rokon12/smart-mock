// Smart Mock Server - JavaScript functionality

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