// Define container templates as a separate module
const containerTemplates = {
    'ubuntu': {
        image: 'ubuntu:22.04',
        port: 22,
        // env is intentionally missing
        cmd: 'sleep infinity',
        docPath: '' // No documentation available yet
    },

    'nginx': {
        image: 'nginx:latest',
        port: 80,
        env: {
            'NGINX_HOST': 'localhost'
        },
        // cmd is intentionally missing
        docPath: '' // No documentation available yet
    },

    'mysql': {
        image: 'mysql:8.0',
        port: 3306,
        env: {
            'MYSQL_ROOT_PASSWORD': '',
            'MYSQL_DATABASE': 'mydb'
        },
        // cmd is intentionally missing
        docPath: '' // No documentation available yet
    },

    'oracle database': {
        image: 'gvenzl/oracle-xe:21',
        port: 1521,
        env: {
            'ORACLE_PASSWORD': ''
        },
        // cmd is intentionally missing
        docPath: 'assets/doc/oracle_database.md'
    },
};