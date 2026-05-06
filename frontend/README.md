# DisputeHub Frontend

A production-ready React + TypeScript frontend for the DisputeHub transaction dispute management system.

## Features

- **Authentication**: JWT-based authentication with persistent sessions
- **Role-Based Access**: Separate dashboards for Customers and Admins
- **Customer Portal**:
  - View all transactions
  - Create disputes for eligible transactions
  - Track dispute status and history
- **Admin Portal**:
  - View all disputes across all customers
  - Update dispute statuses
  - Filter disputes by status
  - Real-time statistics dashboard
- **Production-Ready**:
  - TypeScript for type safety
  - React Query for efficient data fetching and caching
  - Tailwind CSS for responsive, modern UI
  - Comprehensive error handling
  - Loading states and optimistic updates

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **React Router v6** - Client-side routing
- **TanStack React Query** - Data fetching and caching
- **Axios** - HTTP client with interceptors
- **Tailwind CSS** - Utility-first CSS framework
- **Vite** - Fast build tool and dev server

## Project Structure

```
frontend/
├── public/
│   └── vite.svg                    # Favicon
├── src/
│   ├── components/
│   │   ├── Login.tsx              # Login page
│   │   ├── Register.tsx           # Registration page
│   │   ├── CustomerDashboard.tsx  # Customer portal
│   │   └── AdminDashboard.tsx     # Admin portal
│   ├── contexts/
│   │   └── AuthContext.tsx        # Authentication context
│   ├── services/
│   │   └── api.ts                 # API service with interceptors
│   ├── types/
│   │   └── index.ts               # TypeScript type definitions
│   ├── App.tsx                    # Main app component with routing
│   ├── main.tsx                   # Application entry point
│   └── index.css                  # Global styles
├── index.html                     # HTML template
├── package.json                   # Dependencies
├── tsconfig.json                  # TypeScript config
├── vite.config.ts                 # Vite config
├── tailwind.config.js             # Tailwind config
└── postcss.config.js              # PostCSS config
```

## Prerequisites

- Node.js 18+ and npm/yarn
- Backend API running on http://localhost:8080

## Installation

1. Install dependencies:
```bash
npm install
```

2. Ensure the backend API is running on port 8080

## Development

Start the development server:
```bash
npm run dev
```

The application will be available at http://localhost:3000

## Build for Production

```bash
npm run build
```

The production-ready files will be in the `dist/` directory.

## Preview Production Build

```bash
npm run preview
```

## API Integration

The frontend connects to the backend API at `http://localhost:8080`. The following endpoints are used:

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login

### Transactions (Customer)
- `GET /api/transactions` - Get all user transactions
- `GET /api/transactions/disputeable` - Get disputable transactions

### Disputes
- `POST /api/disputes` - Create a new dispute (Customer)
- `GET /api/disputes/my-disputes` - Get user's disputes (Customer)
- `GET /api/disputes` - Get all disputes (Admin)
- `PUT /api/disputes/{id}/status` - Update dispute status (Admin)

## Authentication Flow

1. User logs in or registers
2. Backend returns JWT token and user object
3. Token is stored in localStorage
4. All subsequent API requests include the token in Authorization header
5. Axios interceptor automatically adds token to requests
6. On 401 responses, user is redirected to login

## User Roles

### Customer
- View personal transactions
- Create disputes for eligible transactions
- View and track their own disputes

### Admin
- View all disputes from all customers
- Update dispute statuses
- Access to statistics dashboard
- Filter disputes by status

## Key Features

### Protected Routes
Routes are protected based on authentication status. Unauthenticated users are redirected to login.

### Automatic Token Management
JWT tokens are automatically attached to API requests via Axios interceptors.

### Error Handling
Comprehensive error handling with user-friendly error messages.

### Responsive Design
Fully responsive UI that works on desktop, tablet, and mobile devices.

### Loading States
All data fetching operations display loading indicators.

### Optimistic Updates
React Query provides optimistic updates and automatic cache invalidation.

## Testing Credentials

After seeding the database, you can use:

**Customer Account:**
- Email: john.doe@example.com
- Password: Password@123

**Admin Account:**
- Email: admin@disputehub.com
- Password: Admin@123

## Environment Variables

To change the API base URL, modify the `API_BASE_URL` constant in `src/services/api.ts`:

```typescript
const API_BASE_URL = 'http://localhost:8080';
```

For production, consider using environment variables with Vite:

```typescript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## License

This project is part of a technical assessment.
