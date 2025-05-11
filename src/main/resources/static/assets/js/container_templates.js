// Define container templates as a separate module
const containerTemplates = {
    'ubuntu': {
        image: 'ubuntu:22.04',
        port: 22,
        // env is intentionally missing
        cmd: 'sleep infinity'
    },

    'nginx': {
        image: 'nginx:latest',
        port: 80,
        env: {
            'NGINX_HOST': 'localhost'
        }
        // cmd is intentionally missing
    },

    'mysql': {
        image: 'mysql:8.0',
        port: 3306,
        env: {
            'MYSQL_ROOT_PASSWORD': 'password',
            'MYSQL_DATABASE': 'mydb'
        }
        // cmd is intentionally missing
    },

    'oracle database': {
        image: 'gvenzl/oracle-xe:21',
        port: 1521,
        env: {
            'ORACLE_PWD': 'password'
        }
        // cmd is intentionally missing
    },
};