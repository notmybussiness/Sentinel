# AWS Deployment Roadmap

## Current State vs Target

### ✅ Current (Phase 1)
- Monorepo with Next.js 14 + Spring Boot
- Domain-driven architecture (user, portfolio, market, common)
- Local development environment working
- Docker configurations ready
- Basic authentication and CRUD operations

### 🎯 Target (Phase 2-3)
- AWS production deployment (~$16/month)
- CI/CD with GitHub Actions + Gemini reviews
- Terraform Infrastructure as Code
- Real-world professional workflow

## 4-Week Implementation Plan

### Week 1: Foundation
- [ ] AWS account setup + billing alerts
- [ ] Terraform state backend (S3 + DynamoDB)
- [ ] Repository secrets configuration
- [ ] Manual deployment testing

### Week 2: Infrastructure
- [ ] Terraform modules (VPC, RDS, ALB, EC2)
- [ ] Security groups and IAM roles
- [ ] Environment-specific configurations
- [ ] Infrastructure validation

### Week 3: CI/CD Pipeline
- [ ] GitHub Actions workflow setup
- [ ] Gemini code review integration
- [ ] Staging auto-deploy
- [ ] Production manual-deploy

### Week 4: Production Ready
- [ ] SSL certificates + security hardening
- [ ] Monitoring and alerting setup
- [ ] Performance optimization
- [ ] Documentation completion

## Cost Structure (Monthly)
| Service | Cost | Justification |
|---------|------|---------------|
| EC2 t2.micro | Free | 750 hours/month free tier |
| RDS db.t3.micro | Free | 750 hours/month free tier |
| ALB | $16.20 | Only paid service, needed for HTTPS |
| **Total** | **$16.20** | **Optimized for learning** |

## Domain Development Strategy

### Problem Solve Sessions
- Each feature development documented in `/docs/problem-solve/YYYY-MM-DD-feature.md`
- Use `/docs/domains/[domain].md` for temporary context during development
- Delete domain context after session completion

### Development Workflow
1. Create feature branch: `feature/domain-feature`
2. Create domain context: `docs/domains/market.md` (temporary)
3. Implement using TDD approach
4. Document session: `docs/problem-solve/2025-09-07-market-realtime.md`
5. PR → Gemini review → Peer review → Merge
6. Clean up domain context file

## Next Steps for Planning

### Immediate Decisions Needed:
1. **AWS Region**: us-east-1 (cheapest) vs us-west-2 (closer)?
2. **CI/CD Tool**: GitHub Actions (integrated) vs Jenkins (self-hosted)?
3. **Database**: Keep PostgreSQL + Redis or simplify?
4. **Deployment**: Docker Swarm vs plain EC2?

### Technical Choices:
1. **IaC Tool**: Terraform (industry standard) vs AWS CDK vs Pulumi?
2. **Monitoring**: CloudWatch (simple) vs ELK stack (comprehensive)?
3. **SSL**: AWS Certificate Manager (free) vs Let's Encrypt?
4. **Domain**: Use AWS Route 53 or existing DNS?

---
*Ready to discuss and refine this roadmap. What aspects should we prioritize or modify?*