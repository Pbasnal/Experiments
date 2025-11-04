"""add schedule to series

Revision ID: aaaaddscheduletoseries
Revises: 60585fcc1849
Create Date: 2025-07-02
"""

from alembic import op
import sqlalchemy as sa

revision = 'aaaaddscheduletoseries'
down_revision = '60585fcc1849'
branch_labels = None
depends_on = None

def upgrade():
    with op.batch_alter_table('series') as batch_op:
        batch_op.add_column(sa.Column('schedule', sa.String(length=20)))

def downgrade():
    with op.batch_alter_table('series') as batch_op:
        batch_op.drop_column('schedule') 